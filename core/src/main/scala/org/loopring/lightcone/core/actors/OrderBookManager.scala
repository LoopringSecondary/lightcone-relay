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
import com.google.inject.Inject
import org.loopring.lightcone.core.managing.NodeData
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.core.utils.{ OrderBookManagerHelperImpl, OrderWithStatus }
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.proto.orderbook.{ CrossingOrderSets, GetCrossingOrderSets }

import scala.concurrent.ExecutionContext

object OrderBookManager
  extends base.Deployable[OrderBookManagerSettings] {
  val name = "order_book_manager"
  override val isSingleton = true

  def getCommon(s: OrderBookManagerSettings) =
    base.CommonSettings(Some(s.id), s.roles, 1)
}

class OrderBookManager @Inject() ()(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends Actor {
  var settings: OrderBookManagerSettings = null

  var id = settings.id
  lazy val orderAccessor = Routers.orderAccessor
  lazy val readCoordinator = Routers.orderReadCoordinator

  def marketConfig(): MarketConfig = NodeData.getMarketConfigById(id)
  val managerHelper = new OrderBookManagerHelperImpl(marketConfig())

  DistributedPubSub(context.system).mediator ! Subscribe(CacheObsoleter.name, self)

  def receive: Receive = {
    case settings: OrderBookManagerSettings =>
      this.settings = settings
      val query = OrderQuery(market = "", delegateAddress = settings.delegate, status = Seq(""), orderType = "")
      //todo:await
      val f = managerHelper.resetOrders(query)

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
      val query = OrderQuery(market = "", delegateAddress = settings.delegate, status = Seq(""), orderType = "")
      managerHelper.resetOrders(query)
    case _ =>
  }

  val canMatching: PartialFunction[OrderWithStatus, Boolean] = {
    case _ => true //todo:
  }

}