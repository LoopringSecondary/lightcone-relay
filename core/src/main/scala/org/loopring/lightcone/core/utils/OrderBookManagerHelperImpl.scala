/*
 * Copyright 2018 lightcore-relay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.loopring.lightcone.core.utils

import akka.actor._
import akka.pattern._
import akka.util._
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.lib.math._
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order.OrderLevel1Status._
import org.loopring.lightcone.proto.order._

import scala.collection.mutable
import scala.concurrent._

case class TokenOrders(
    tokenAOrders: Set[OrderWithStatus] = Set.empty,
    tokenBOrders: Set[OrderWithStatus] = Set.empty
)

case class OrderWithStatus(
    order: Order,
    postponed: Long
)

class OrderBookManagerHelperImpl(
    orderAccessor: ActorRef,
    readCoordinator: ActorRef,
    marketConfig: MarketConfig
)(implicit
    ec: ExecutionContext,
    timeout: Timeout
) extends OrderBookManagerHelper {
  //key:AmountA/AmountB

  var orderbook = mutable.TreeMap[Rational, TokenOrders]()

  override def updateOrder(updatedOrder: UpdatedOrder): Unit = {
    val rawOrder = updatedOrder.order.get.rawOrder.get
    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    //根据o.status的状态，执行不同的操作，新增、更新、删除、暂停等
    updatedOrder.order.get.status match {
      case None ⇒
        this.addOrder(OrderWithStatus(updatedOrder.order.get, updatedOrder.postponed))

      case Some(OrderStatus(ORDER_STATUS_FULL, _, _)) ⇒ //完全成交，需要从orderbook中删除
        this.delOrder(updatedOrder.order.get.rawOrder.get)

      case Some(OrderStatus(ORDER_STATUS_SOFT_CANCELLED, _, _)) ⇒ //软删除，从orderbook中删除
        this.delOrder(updatedOrder.order.get.rawOrder.get)

      case Some(OrderStatus(ORDER_STATUS_HARD_CANCELLED, _, _)) ⇒ //硬删除，从orderbook中删除
        this.delOrder(updatedOrder.order.get.rawOrder.get)

      case Some(OrderStatus(ORDER_STATUS_EXPIRED, _, _)) ⇒ //过期，删除
        this.delOrder(updatedOrder.order.get.rawOrder.get)

      case Some(OrderStatus(ORDER_STATUS_NEW, _, _)) ⇒ //新订单，加入到orderbook
        this.addOrder(OrderWithStatus(updatedOrder.order.get, 0l))

      case Some(_) ⇒ // TODO(hongyu)
    }
  }

  private def addOrder(orderWithStatus: OrderWithStatus) = {
    val rawOrder = orderWithStatus.order.rawOrder.get
    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    orderbook.synchronized {
      //      val orderWithStatus = OrderWithStatus(order.order.get, order.postponed)
      var tokenOrders = orderbook.getOrElseUpdate(sellPrice, TokenOrders())
      tokenOrders =
        if (rawOrder.tokenS == marketConfig.tokenA) {
          val tokenAOrders = tokenOrders
            .tokenAOrders
            .filter(_.order.rawOrder.get.hash != rawOrder.hash)
          tokenOrders.copy(tokenAOrders = tokenAOrders + orderWithStatus)
        } else {
          val tokenBOrders = tokenOrders
            .tokenBOrders
            .filter(_.order.rawOrder.get.hash != rawOrder.hash)
          tokenOrders.copy(tokenBOrders = tokenBOrders + orderWithStatus)
        }
      orderbook.put(sellPrice, tokenOrders)
    }
  }

  private def delOrder(rawOrder: RawOrder) = {
    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    orderbook.synchronized {
      var tokenOrders = orderbook.getOrElseUpdate(sellPrice, TokenOrders())
      tokenOrders =
        if (rawOrder.tokenS == marketConfig.tokenA) {
          tokenOrders.copy(tokenAOrders = tokenOrders
            .tokenAOrders
            .filter(_.order.rawOrder.get.hash != rawOrder.hash))
        } else {
          tokenOrders.copy(tokenBOrders = tokenOrders
            .tokenBOrders
            .filter(_.order.rawOrder.get.hash != rawOrder.hash))
        }
      orderbook.put(sellPrice, tokenOrders)
    }
  }

  override def crossingOrdersBetweenPrices(
    minPrice: Rational,
    maxPrice: Rational
  ): TokenOrders = {
    orderbook.filter {
      case (sellPrice, _) ⇒ sellPrice >= minPrice && sellPrice <= maxPrice
    }
      .values
      .reduceLeft[TokenOrders] {
        (res, tokenOrders) ⇒
          res.copy(
            tokenAOrders = res.tokenAOrders ++ tokenOrders.tokenAOrders,
            tokenBOrders = res.tokenBOrders ++ tokenOrders.tokenAOrders
          )
      }
  }

  override def crossingPrices(
    canBeMatched: PartialFunction[OrderWithStatus, Boolean]
  ): (Rational, Rational) = {
    //可以成交的最大和最小价格
    var minPrice: Rational = null
    var maxPrice: Rational = null
    orderbook.foreach {
      case (sellPrice, tokenOrders) ⇒
        if (null == minPrice && tokenOrders.tokenAOrders.count(canBeMatched) > 1) {
          minPrice = sellPrice
          maxPrice = sellPrice
        }
        if (tokenOrders.tokenBOrders.count(canBeMatched) > 1) {
          maxPrice = sellPrice
        }
    }
    (minPrice, maxPrice)
  }

  override def resetOrders(query: OrderQuery): Future[Unit] = for {
    orderQuery ← Future.successful(query)
    res ← (orderAccessor ? GetTopOrders(query = Some(orderQuery))).mapTo[TopOrders]
  } yield {
    orderbook = mutable.TreeMap[Rational, TokenOrders]()
    res.order foreach { o ⇒
      val sellPrice = Rational(
        o.rawOrder.get.amountS.asBigInt,
        o.rawOrder.get.amountB.asBigInt
      )
      //todo:确认order需要如何转换成updatedOrder
      val updatedOrder = UpdatedOrder()
      updateOrder(updatedOrder)
    }
  }

  override def purgeOrders(purge: Purge.AllOrderForAddress): Future[Unit] = for {
    orderHashes ← Future.successful {
      orderbook.flatMap {
        case (_, tokenOrders) ⇒
          val tokenAOrders = tokenOrders
            .tokenAOrders
            .filter(_.order.rawOrder.get.owner == purge.address)
            .map(_.order.rawOrder.get.hash)

          val tokenBOrders = tokenOrders
            .tokenBOrders
            .filter(_.order.rawOrder.get.owner == purge.address)
            .map(_.order.rawOrder.get.hash)

          tokenAOrders ++ tokenBOrders
      }.toSeq
    }
    _ ← purgeOrders(orderHashes)
  } yield Unit

  override def purgeOrders(purge: Purge.AllForAddresses): Future[Unit] = for {
    orderHashes ← Future.successful {
      orderbook.flatMap {
        case (_, tokenOrders) ⇒
          val tokenAOrders = tokenOrders.tokenAOrders
            .filter { o ⇒
              purge.addresses.contains(o.order.rawOrder.get.owner)
            }
            .map(_.order.rawOrder.get.hash)

          val tokenBOrders = tokenOrders.tokenBOrders
            .filter { o ⇒
              purge.addresses.contains(o.order.rawOrder.get.owner)
            }
            .map(_.order.rawOrder.get.hash)

          tokenAOrders ++ tokenBOrders
      }.toSeq
    }
    _ ← purgeOrders(orderHashes)
  } yield Unit

  override def purgeOrders(purge: Purge.All): Future[Unit] =
    resetOrders(query = OrderQuery()) //todo:

  override def purgeOrders(purge: Purge.AllAfterBlock): Future[Unit] = ???

  override def purgeOrders(orderHashes: Seq[String]): Future[Unit] = for {
    updatedOrdersAny ← readCoordinator ? UpdateOrdersById(orderHashes = orderHashes)
  } yield {
    updatedOrdersAny match {
      case updatedOrders: UpdatedOrders ⇒ updatedOrders.orders.foreach(updateOrder)
      case _ ⇒
    }
  }
}
