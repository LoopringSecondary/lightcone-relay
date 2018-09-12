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
import akka.util.Timeout
import org.loopring.lightcone.core.managing.NodeData
import org.loopring.lightcone.core.utils.{OrderBookManagerHelperImpl, OrderWithStatus}
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.proto.orderbook.{CrossingOrderSets, GetCrossingOrderSets, GetOrderBookReq, GetOrderBookResp}

import scala.concurrent.{Await, ExecutionContext}

object OrderBookManager
  extends base.Deployable[OrderBookManagerSettings] {
  val name = "order_book_manager"
  override val isSingleton = true

  def getCommon(s: OrderBookManagerSettings) =
    base.CommonSettings(Some(s.id), s.roles, 1)
}

class OrderBookManager()(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends Actor {
  var settings: OrderBookManagerSettings = null

  def marketConfig(): MarketConfig = NodeData.getMarketConfigById(settings.id)
  def resetQuery = OrderQuery(market = settings.id, delegate = settings.delegate, status = Seq(OrderLevel1Status.ORDER_STATUS_NEW.name), orderType = OrderType.MARKET.name)

  val managerHelper = new OrderBookManagerHelperImpl(marketConfig())

  DistributedPubSub(context.system).mediator ! Subscribe(CacheObsoleter.name, self)

  def receive: Receive = {
    case settings: OrderBookManagerSettings =>
      this.settings = settings
      //orderbookmanager依赖于manageHelper的数据完整，需要等待初始化完成
      Await.result(managerHelper.resetOrders(resetQuery), timeout.duration)

    case m:GetOrderBookReq =>
        sender() ! GetOrderBookResp() //todo:

    case m: GetCrossingOrderSets =>
      val (minPrice, maxPrice) = managerHelper.crossingPrices(canMatching)
      val tokenOrders = managerHelper.crossingOrdersBetweenPrices(minPrice, maxPrice)
      sender() ! CrossingOrderSets(
        sellTokenAOrders = tokenOrders.tokenAOrders.filter(canMatching).map(_.order).toSeq,
        sellTokenBOrders = tokenOrders.tokenBOrders.filter(canMatching).map(_.order).toSeq)

    case m: UpdatedOrders =>
      m.orders.foreach(managerHelper.updateOrder)

    case m: Purge.Order =>
      managerHelper.purgeOrders(Seq(m.orderHash))

    case m: Purge.AllOrderForAddress =>
      managerHelper.purgeOrders(m)

    case m: Purge.AllForAddresses =>
      managerHelper.purgeOrders(m)

    case m: Purge.AllAfterBlock =>
      managerHelper.purgeOrders(m)

    case m: Purge.All =>
      //      managerHelper.purgeOrders(m)
      //      managerHelper.resetOrders(query)
      Await.result(managerHelper.resetOrders(resetQuery), timeout.duration)
    case _ =>
  }

  val canMatching: PartialFunction[OrderWithStatus, Boolean] = {
    case OrderWithStatus(order, postponed) =>
      order.status match {
        case None => true
        case Some(OrderStatus(level1Status, level2Status, level3Status)) =>
          val currentTime = System.currentTimeMillis
          if (level1Status == OrderLevel1Status.ORDER_STATUS_NEW
            && postponed <= currentTime
            && level3Status == OrderLevel3Status.ORDER_STATUS_ACTIVE)
            //todo:增加灰尘单的过滤，需要依赖于marketcap
            true
          else
            false
      }
    case _ => false
  }

}