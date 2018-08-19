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
import org.loopring.lightcone.data.deployment._
import org.loopring.lightcone.proto.orderbook.{ CrossingOrderSets, GetCrossingOrderSets }
import org.loopring.lightcone.proto.order.{ DeferOrder, MarkOrdersBeingMatched, MarkOrdersDeferred, MarkOrdersSettling }
import org.loopring.lightcone.proto.ring._

import scala.concurrent.Future
import scala.concurrent.duration._

object RingFinder
  extends base.Deployable[RingFinderSettings] {
  val name = "ring_finder"
  val isSingleton = true

  def props = Props(classOf[RingFinder]).withDispatcher("ring-dispatcher")

  def getCommon(s: RingFinderSettings) =
    base.CommonSettings(s.id, s.roles, 1)
}

class RingFinder(
  ringMiner: ActorRef,
  orderManager: ActorRef,
  orderBookManager: ActorRef)(implicit timeout: Timeout)
  extends Actor
  with ActorLogging {
  import context.dispatcher
  var finderSettings: Option[RingFinderSettings] = None

  private def nextFindRound() = {
    finderSettings.map(s =>
      context.system.scheduler.scheduleOnce(
        s.scheduleDelay seconds,
        self,
        GetCrossingOrderSets(tokenA = s.tokenA, tokenB = s.tokenB)))
  }

  def receive: Receive = {
    case settings: RingFinderSettings =>
      finderSettings = Some(settings)
      nextFindRound()

    case getCrossingOrderSets: GetCrossingOrderSets => for {
      crossingOrderSets <- orderBookManager ? getCrossingOrderSets recover {
        case exception: AskTimeoutException ⇒ exception
      }
    } yield {
      crossingOrderSets match {
        case orders: CrossingOrderSets =>
          orderManager ! MarkOrdersBeingMatched(orders.sellTokenAOrders ++ orders.sellTokenBOrders)
          ringMiner ! RingCandidates()
          orderManager ! MarkOrdersDeferred()
        case e: AskTimeoutException =>
      }
      nextFindRound()
    }

    case ringSettlementDecisions: NotifyRingSettlementDecisions =>
      orderManager ! MarkOrdersDeferred(deferOrders =
        ringSettlementDecisions.ringSettlementDecisions
          .filter(r => r.decision == SettlementDecision.UnSettled)
          .flatMap(r => r.orders.map(o => DeferOrder(order = Some(o), deferedTime = 100))))

      orderManager ! MarkOrdersSettling(orders = ringSettlementDecisions.ringSettlementDecisions
        .filter(r => r.decision == SettlementDecision.Settled)
        .flatMap(r => r.orders))

    case getFinderRingCandidates: GetRingCandidates =>
      sender() ! RingCandidates()

    case _ =>

  }
}