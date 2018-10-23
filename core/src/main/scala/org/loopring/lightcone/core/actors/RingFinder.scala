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
import akka.pattern.{ AskTimeoutException, ask }
import akka.util.Timeout
import org.loopring.lightcone.core.actors.base.RepeatedJobActor
import org.loopring.lightcone.core.managing.NodeData
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.proto.orderbook.{ CrossingOrderSets, GetCrossingOrderSets }
import org.loopring.lightcone.proto.ring._

import scala.concurrent.{ ExecutionContext, _ }

object RingFinder
  extends base.Deployable[RingFinderSettings] {
  val name = "ring_finder"
  override val isSingleton = true

  def getCommon(s: RingFinderSettings) =
    base.CommonSettings(Some(s.id), s.roles, 1)
}

class RingFinder()(implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends RepeatedJobActor
  with ActorLogging {

  var settings: RingFinderSettings = null

  lazy val id = settings.id
  lazy val orderBookManager: ActorRef = Routers.orderBookManager(id)
  lazy val updateCoordinator: ActorRef = Routers.orderUpdateCoordinator

  def marketConfig(): MarketConfig = NodeData.getMarketConfigById(id)

  //todo:需要确定deferred time
  override def receive: Receive = super.receive orElse {
    case settings: RingFinderSettings ⇒
      this.settings = settings
      initAndStartNextRound(settings.scheduleDelay)

    case m: NotifyRingSettlementDecisions ⇒
      updateCoordinator ! MarkOrdersDeferred(deferOrders =
        m.ringSettlementDecisions
          .filter(r ⇒ r.decision == SettlementDecision.UnSettled)
          .flatMap(r ⇒ r.ordersSettling.map(o ⇒ DeferOrder(orderHash = o.orderHash, deferredTime = 100))))

      updateCoordinator ! MarkOrdersSettling(ordersSettling = m.ringSettlementDecisions
        .filter(r ⇒ r.decision == SettlementDecision.Settled)
        .flatMap(r ⇒ r.ordersSettling))

    case getFinderRingCandidates: GetRingCandidates ⇒
      sender() ! RingCandidates() //todo:

    case m: RingSettlementDecision if m.decision == SettlementDecision.UnSettled ⇒
      updateCoordinator ! MarkOrdersDeferred(deferOrders =
        m.ordersSettling.map(o ⇒ DeferOrder(orderHash = o.orderHash, deferredTime = 100)))

  }

  def handleRepeatedJob() = for {
    lastTime ← Future.successful(System.currentTimeMillis)
    getCrossingOrderSets = GetCrossingOrderSets(tokenA = marketConfig.tokenA, tokenB = marketConfig.tokenB)
    crossingOrderSets ← orderBookManager ? getCrossingOrderSets recover {
      case exception: AskTimeoutException ⇒ exception
    }
  } yield {
    crossingOrderSets match {
      case orders: CrossingOrderSets ⇒
        updateCoordinator ! MarkOrdersBeingMatched(
          ordersBeingMatched =
            (orders.sellTokenAOrders ++ orders.sellTokenBOrders)
              .map(o ⇒ OrderBeingMatched(o.getRawOrder.getEssential.hash))
        )
      case e: AskTimeoutException ⇒
    }
  }
}
