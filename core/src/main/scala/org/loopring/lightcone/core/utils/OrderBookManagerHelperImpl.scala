/*
 * Copyright 2018 Loopring Foundation
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
import org.loopring.lightcone.proto.order.UpdatedOrder.Updated
import org.loopring.lightcone.proto.order._

import scala.collection.mutable
import scala.concurrent._

case class TokenOrders(
    tokenAOrders: Set[String] = Set.empty,
    tokenBOrders: Set[String] = Set.empty
)

case class OrderWithStatus(
    order: Order,
    postponed: Long
)

class OrderBook(marketConfig: MarketConfig) {
  var orderOfPriceMap = mutable.TreeMap[Rational, TokenOrders]()
  var orders = mutable.HashMap[String, OrderWithStatus]()

  def updateOrAddOrder(orderWithStatus: OrderWithStatus) = {
    val rawOrder = orderWithStatus.order.rawOrder.get
    orders.synchronized(orders.put(rawOrder.hash.toLowerCase, orderWithStatus))
    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    orderOfPriceMap.synchronized {
      //      val orderWithStatus = OrderWithStatus(order.order.get, order.postponed)
      var tokenOrders = orderOfPriceMap.getOrElseUpdate(sellPrice, TokenOrders())
      tokenOrders =
        if (rawOrder.tokenS == marketConfig.tokenA) {
          val tokenAOrders = tokenOrders
            .tokenAOrders
            .filter(_ != rawOrder.hash)
          tokenOrders.copy(tokenAOrders = tokenAOrders + rawOrder.hash.toLowerCase)
        } else {
          val tokenBOrders = tokenOrders
            .tokenBOrders
            .filter(_ != rawOrder.hash)
          tokenOrders.copy(tokenBOrders = tokenBOrders + rawOrder.hash.toLowerCase)
        }
      orderOfPriceMap.put(sellPrice, tokenOrders)
    }
  }

  def delOrder(rawOrder: RawOrder) = {
    orders.synchronized(orders.remove(rawOrder.hash.toLowerCase))
    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    orderOfPriceMap.synchronized {
      var tokenOrders = orderOfPriceMap.getOrElseUpdate(sellPrice, TokenOrders())
      tokenOrders =
        if (rawOrder.tokenS == marketConfig.tokenA) {
          tokenOrders.copy(tokenAOrders = tokenOrders
            .tokenAOrders
            .filter(_ != rawOrder.hash))
        } else {
          tokenOrders.copy(tokenBOrders = tokenOrders
            .tokenBOrders
            .filter(_ != rawOrder.hash))
        }
      orderOfPriceMap.put(sellPrice, tokenOrders)
    }
  }

  def getOrder(hash: String) = {
    this.orders(hash.toLowerCase).order
  }

  def updateOrderLevel3Status(hash: String, level3Status: OrderLevel3Status) = {
    this.orders.synchronized {
      this.orders.get(hash) match {
        case Some(orderWithStatus) ⇒
          val orderStatus = orderWithStatus.order.status match {
            case Some(status) ⇒ status.copy(level3Status = level3Status)
            case None         ⇒ OrderStatus(level3Status = level3Status)
          }
          val order = orderWithStatus.order.copy(status = Some(orderStatus))
          this.orders.put(hash.toLowerCase, orderWithStatus.copy(order = order))
        case _ ⇒
      }
    }
  }
}

class OrderBookManagerHelperImpl(
    orderAccessor: ActorRef,
    readCoordinator: ActorRef,
    marketConfig: MarketConfig
)(implicit
    ec: ExecutionContext,
    timeout: Timeout
) extends OrderBookManagerHelper {
  //key:AmountA/AmountB

  //  var orderbookBak = mutable.TreeMap[Rational, TokenOrders]()
  var orderbook = new OrderBook(marketConfig)

  override def updateOrder(updatedOrder: UpdatedOrder): Unit = {
    updatedOrder.updated match {
      case Updated.Order(order) ⇒
        val rawOrder = order.rawOrder.get
        val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
        //根据o.status的状态，执行不同的操作，新增、更新、删除、暂停等
        order.status match {
          case Some(OrderStatus(ORDER_STATUS_FULL, _, _)) ⇒ //完全成交，需要从orderbook中删除
            orderbook.delOrder(order.rawOrder.get)
          case Some(OrderStatus(ORDER_STATUS_SOFT_CANCELLED, _, _)) ⇒ //软删除，从orderbook中删除
            orderbook.delOrder(order.rawOrder.get)
          case Some(OrderStatus(ORDER_STATUS_HARD_CANCELLED, _, _)) ⇒ //硬删除，从orderbook中删除
            orderbook.delOrder(order.rawOrder.get)
          case Some(OrderStatus(ORDER_STATUS_EXPIRED, _, _)) ⇒ //过期，删除
            orderbook.delOrder(order.rawOrder.get)
          case Some(OrderStatus(ORDER_STATUS_NEW, _, _)) ⇒ //新订单，加入到orderbook
            orderbook.updateOrAddOrder(OrderWithStatus(order, 0l))
          case _ ⇒
            orderbook.updateOrAddOrder(OrderWithStatus(order, 0l))
        }

      case Updated.DeferOrder(deferOrder) ⇒
        val orderWithStatus = OrderWithStatus(
          orderbook.getOrder(deferOrder.orderHash),
          deferOrder.deferredTime
        )
        orderbook.updateOrAddOrder(orderWithStatus)

      case Updated.OrderBeingMatched(orderBeingMatched) ⇒
        orderbook.updateOrderLevel3Status(orderBeingMatched.orderHash, OrderLevel3Status.ORDER_STATUS_MATCHING)

      case Updated.OrderSettling(orderSettling) ⇒
        orderbook.updateOrderLevel3Status(orderSettling.orderHash, OrderLevel3Status.ORDER_STATUS_SETTLEING)
    }

  }

  override def crossingOrdersBetweenPrices(
    minPrice: Rational,
    maxPrice: Rational
  ): TokenOrders = {
    orderbook.orderOfPriceMap.filter {
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
    orderbook.orderOfPriceMap.foreach {
      case (sellPrice, tokenOrders) ⇒
        if (null == minPrice &&
          tokenOrders
          .tokenAOrders
          .map(orderbook.orders(_))
          .count(canBeMatched) > 1) {
          minPrice = sellPrice
          maxPrice = sellPrice
        }
        if (tokenOrders
          .tokenBOrders
          .map(orderbook.orders(_))
          .count(canBeMatched) > 1) {
          maxPrice = sellPrice
        }
    }
    (minPrice, maxPrice)
  }

  override def resetOrders(query: OrderQuery): Future[Unit] = for {
    orderQuery ← Future.successful(query)
    res ← (orderAccessor ? GetTopOrders(query = Some(orderQuery))).mapTo[TopOrders]
  } yield {
    orderbook = new OrderBook(marketConfig)
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
      orderbook.orderOfPriceMap.flatMap {
        case (_, tokenOrders) ⇒
          val tokenAOrders = tokenOrders
            .tokenAOrders
            .filter { hash ⇒
              val rawOrder = orderbook.getOrder(hash).rawOrder.get
              rawOrder.owner.equalsIgnoreCase(purge.address)
            }

          val tokenBOrders = tokenOrders
            .tokenBOrders
            .filter { hash ⇒
              val rawOrder = orderbook.getOrder(hash).rawOrder.get
              rawOrder.owner.equalsIgnoreCase(purge.address)
            }

          tokenAOrders ++ tokenBOrders
      }.toSeq
    }
    _ ← purgeOrders(orderHashes)
  } yield Unit

  override def purgeOrders(purge: Purge.AllForAddresses): Future[Unit] = for {
    orderHashes ← Future.successful {
      orderbook.orderOfPriceMap.flatMap {
        case (_, tokenOrders) ⇒
          val tokenAOrders = tokenOrders.tokenAOrders
            .filter { hash ⇒
              val rawOrder = orderbook.getOrder(hash).rawOrder.get
              purge.addresses.exists { addr ⇒
                addr.equalsIgnoreCase(rawOrder.owner)
              }
            }

          val tokenBOrders = tokenOrders.tokenBOrders
            .filter { hash ⇒
              val rawOrder = orderbook.getOrder(hash).rawOrder.get
              purge.addresses.exists { addr ⇒
                addr.equalsIgnoreCase(rawOrder.owner)
              }
            }
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
