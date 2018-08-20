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
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.data.deployment._
import org.loopring.lightcone.proto.common.StartNewRound
import org.loopring.lightcone.proto.ring.{ GetRingCandidates, NotifyRingSettlementDecisions, RingCandidates }

import scala.concurrent.Future

object RingMiner
  extends base.Deployable[RingMinerSettings] {
  val name = "ring_miner"
  val isSingleton = true

  def props = Props(classOf[RingMiner]).withDispatcher("ring-dispatcher")

  def getCommon(s: RingMinerSettings) =
    base.CommonSettings(s.address, s.roles, 1)
}

class RingMiner()(implicit timout: Timeout) extends RoundActor {
  import context.dispatcher
  var finders: Seq[ActorRef] = Seq.empty
  val balanceManager = Routers.balanceManager
  val ethereumAccessor = Routers.ethereumAccessor

  def receive: Receive = {
    case settings: RingMinerSettings =>
      finders = settings.marketIds.map { id => Routers.ringFinder(id) }
      initAndStartNextRound(settings.scheduleDelay)

    case m: StartNewRound => for {
      lastTime <- Future { System.currentTimeMillis }
      decisions <- Future sequence finders.map(
        finder => for {
          candidatesOpt <- finder ? GetRingCandidates()
        } yield {
          candidatesOpt match {
            case candidates: RingCandidates =>
              val decisions = decideRingCandidates(candidates)
              finder ! decisions
              decisions
          }
        })

    } yield {
      nextRound(lastTime)
    }

  }

  def decideRingCandidates(candidates: RingCandidates): NotifyRingSettlementDecisions = {
    NotifyRingSettlementDecisions()
  }

}