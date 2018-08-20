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
import org.loopring.lightcone.core.actors.base.RoundActor
import org.loopring.lightcone.core.managing.NodeData
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.data.deployment._
import org.loopring.lightcone.proto.common.StartNewRound
import org.loopring.lightcone.proto.orderbook.{ CrossingOrderSets, GetCrossingOrderSets }
import org.loopring.lightcone.proto.order.{ DeferOrder, MarkOrdersBeingMatched, MarkOrdersDeferred, MarkOrdersSettling }
import org.loopring.lightcone.proto.ring._

import scala.concurrent.Future

object RingFinder
  extends base.Deployable[RingFinderSettings] {
  val name = "ring_finder"
  val isSingleton = true

  def props = Props(classOf[RingFinder]).withDispatcher("ring-dispatcher")

  def getCommon(s: RingFinderSettings) =
    base.CommonSettings(s.id, s.roles, 1)
}

class RingFinder(val id: String)(implicit timeout: Timeout)
  extends RoundActor
  with ActorLogging {
  import context.dispatcher
  var settingsOpt: Option[RingFinderSettings] = None
  val marketConfig: MarketConfig = NodeData.getMarketConfigById(id)
  lazy val orderBookManager: ActorRef = Routers.orderBookManager(id)
  lazy val orderManager: ActorRef = Routers.orderManager(id)

  def receive: Receive = {
    case settings: RingFinderSettings =>
      settingsOpt = Some(settings)
      initAndStartNextRound(settings.scheduleDelay)

    case startNewRound: StartNewRound => for {
      lastTime <- Future { System.currentTimeMillis }
      getCrossingOrderSets = GetCrossingOrderSets(tokenA = marketConfig.tokenA, tokenB = marketConfig.tokenB)
      crossingOrderSets <- orderBookManager ? getCrossingOrderSets recover {
        case exception: AskTimeoutException ⇒ exception
      }
    } yield {
      crossingOrderSets match {
        case orders: CrossingOrderSets =>
          //todo:order结构暂未定，先写死orderhash再替换掉
          orderManager ! MarkOrdersBeingMatched(orderHashes =
            (orders.sellTokenAOrders ++ orders.sellTokenBOrders).map(o => "orderhash"))
        case e: AskTimeoutException =>
      }
      nextRound(lastTime)
    }

    case m: NotifyRingSettlementDecisions =>
      orderManager ! MarkOrdersDeferred(deferOrders =
        m.ringSettlementDecisions
          .filter(r => r.decision == SettlementDecision.UnSettled)
          .flatMap(r => r.orders.map(o => DeferOrder(orderhash = "orderhash", deferredTime = 100))))

      orderManager ! MarkOrdersSettling(orderHashes = m.ringSettlementDecisions
        .filter(r => r.decision == SettlementDecision.Settled)
        .flatMap(r => r.orders.map(o => "orderhash")))

    case getFinderRingCandidates: GetRingCandidates =>
      sender() ! RingCandidates()

  }
}