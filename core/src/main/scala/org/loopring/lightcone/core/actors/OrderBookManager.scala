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

package org.loopring.lightcone.core.actors

import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import org.loopring.lightcone.core.etypes._
import org.loopring.lightcone.core.managing.NodeData
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.proto.orderbook.{CrossingOrderSets, GetCrossingOrderSets}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object OrderBookManager
  extends base.Deployable[OrderBookManagerSettings] {
  val name = "order_book_manager"
  override val isSingleton = true

  def getCommon(s: OrderBookManagerSettings) =
    base.CommonSettings(Some(s.id), s.roles, 1)
}

/**
 * orderbookmanager
 *
 * 订单采用sortedset存储，其中tokena/tokenb 为score，订单hash为value
 *
 *
 * 订单的最新状态需要保存在内存中，但是启动时，从redis中同步订单状态，包括在撮合状态等
 * 没有使用事件溯源，需要确认操作顺序
 *
 *
 *
 * @param cache
 * @param ec
 * @param timeout
 */
class OrderBookManager @Inject() ()(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends Actor {
  var settings: OrderBookManagerSettings = null

  var id = settings.id
  lazy val orderAccessor = Routers.orderAccessor

  //todo:decimal
  val tokenADecimal = 1000000
  val tokenBDecimal = 100000000
  def marketConfig(): MarketConfig = NodeData.getMarketConfigById(id)

  //todo:test hashcode
  //AmountA/AmountB
  var orderbook = mutable.TreeMap[Rational, Map[String, Order]]()
  var orderStatus = mutable.TreeMap[String, String]()

  DistributedPubSub(context.system).mediator ! Subscribe(CacheObsoleter.name, self)

  def receive: Receive = {
    case settings: OrderBookManagerSettings =>
      this.settings = settings
      //todo:await
      initAndGetOrders()

    case m: GetCrossingOrderSets =>
      //可以成交的最大最小价格
      var minPrice: Rational = null
      var maxPrice: Rational = null
      orderbook.foreach { o =>
        {
          if (null == minPrice && o._2.size > 1) {
            minPrice = o._1
          }
          if (o._2.size > 1) {
            maxPrice = o._1
          }
        }
      }
      val orders = orderbook.filter(o => o._1 >= minPrice && o._1 <= maxPrice).flatMap(o1 => {
        val orderMap = o1._2
        orderMap.map(m => {
          (m._1, m._2)
        })
      }).groupBy(_._1).map { m =>
        (m._1, m._2.values.toSeq)
      }

      sender() ! CrossingOrderSets(
        sellTokenAOrders = orders(marketConfig().tokenA),
        sellTokenBOrders = orders(marketConfig().tokenB))

    case m: OrdersUpdated =>
      m.orders.foreach{o => {
        val sellPrice = Rational(o.rawOrder.get.amountS.asBigInt, o.rawOrder.get.amountB.asBigInt)
        orderbook.synchronized(orderbook.put(sellPrice, orderbook.getOrElseUpdate(sellPrice, Map()) ++ Map(o.rawOrder.get.tokenS -> o)))
      }}

    case m: Purge.Order =>

    case m: Purge.AllOrderForAddress =>

    case m: Purge.AllForAddresses =>

    case m: Purge.AllAfterBlock =>

    case m: Purge.All =>

    case _ =>
  }

  def initAndGetOrders(): Future[Unit] = for {
    orderQuery <- Future.successful(OrderQuery(market = "", delegateAddress = settings.delegate, status = Seq(""), orderType = ""))
    res <- (orderAccessor ? GetTopOrders(query = Some(orderQuery))).mapTo[TopOrders]
  } yield {
    res.order map { o =>
      val sellPrice = Rational(o.rawOrder.get.amountS.asBigInt, o.rawOrder.get.amountB.asBigInt)
      orderbook.synchronized(orderbook.put(sellPrice, orderbook.getOrElseUpdate(sellPrice, Map()) ++ Map(o.rawOrder.get.tokenS -> o)))
    }
  }

}