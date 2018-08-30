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
import akka.pattern._
import akka.util.Timeout
import org.loopring.lightcone.core.actors.base._
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.ring._

import scala.concurrent.{ ExecutionContext, Future }

object RingMiner
  extends base.Deployable[RingMinerSettings] {
  val name = "ring_miner"
  override val isSingleton = true

  def getCommon(s: RingMinerSettings) =
    base.CommonSettings(Some(s.address), s.roles, 1)
}

class RingMiner()(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends RepeatedJobActor {
  var marketIds = Seq[String]()

  override def receive: Receive = super.receive orElse {
    case settings: RingMinerSettings =>
      marketIds = settings.marketIds
      initAndStartNextRound(settings.scheduleDelay)
  }

  def handleRepeatedJob() = for {
    finders <- Future.successful(marketIds.map { id => Routers.ringFinder(id) })
    resps <- Future.sequence(finders.map { _ ? GetRingCandidates() })
      .mapTo[Seq[RingCandidates]]
    ringCandidates = RingCandidates(resps.flatMap(_.rings))
    ringsToSettle <- Routers.ringEvaluator ? ringCandidates
  } yield {
    ringsToSettle match {
      case r: RingCandidates =>
        Routers.ringSubmitter ! ringCandidates
        val decisions = decideRingCandidates(ringCandidates.rings, r.rings)
        decisions foreach { decision =>
          val finderId = "" //todo:
          Routers.ringFinder(finderId) ! decision
        }
      case _ =>
    }

  }

  def decideRingCandidates(ringCandidates: Seq[Ring], settledRings: Seq[Ring]): Seq[RingSettlementDecision] = {
    ringCandidates.map(candidate =>
      if (settledRings.contains(candidate))
        RingSettlementDecision(ringHash = candidate.hash, decision = SettlementDecision.Settled, orders = candidate.orders)
      else
        RingSettlementDecision(ringHash = candidate.hash, decision = SettlementDecision.UnSettled, orders = candidate.orders))
  }
}

