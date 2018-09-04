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
import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.core.actors.base._
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.core.utils.{ RingEvaluator, RingSubmitter }
import org.loopring.lightcone.lib.math.Rational
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

class RingMiner(ethClient: EthClient)(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends RepeatedJobActor {
  var marketIds = Seq[String]()
  var evaluator = RingEvaluator()
  var submitter = RingSubmitter(ethClient)
  var finders = Map[String, ActorRef]()

  override def receive: Receive = super.receive orElse {
    case settings: RingMinerSettings =>
      marketIds = settings.marketIds
      if (settings.gasUsedOfOrders.nonEmpty) {
        evaluator = evaluator.copy(gasUsedOfOrders = settings.gasUsedOfOrders)
      }
      if (settings.walletSplit.signum >= 0) {
        evaluator = evaluator.copy(walletSplit = Rational(settings.walletSplit))
      }
      submitter = submitter.copy(
        contract = settings.contract,
        chainId = settings.chainId.toByte,
        keystorePwd = settings.keystorePwd,
        keystoreFile = settings.keystoreFile)
      finders = marketIds.map { id => (id, Routers.ringFinder(id)) }.toMap[String, ActorRef]
      initAndStartNextRound(settings.scheduleDelay)
  }

  def handleRepeatedJob() = for {
    resps <- Future.sequence(finders.map { finder =>
      for {
        resp <- finder._2 ? GetRingCandidates()
      } yield (finder._1, resp)
    }).mapTo[Seq[(String, RingCandidates)]]
    ringFromFinders = resps.flatMap(resp => resp._2.rings.map(ring => (ring.hash, resp._1))).toMap
    ringCandidates = RingCandidates(resps.flatMap(_._2.rings))
    ringsToSettle <- evaluator.getRingCadidatesToSettle(ringCandidates)
  } yield {
    decideRingCandidates(ringCandidates.rings, ringsToSettle.map(_.rawRing))
      .groupBy { decision =>
        ringFromFinders(decision.ringHash)
      }.foreach { decision =>
        val finder = Routers.ringFinder(decision._1)
        finder ! NotifyRingSettlementDecisions(decision._2)
      }
    for (cadidate <- ringsToSettle) {
      submitter.signAndSendTx(cadidate)
    }
  }

  def decideRingCandidates(ringCandidates: Seq[Ring], settledRings: Seq[Ring]): Seq[RingSettlementDecision] = {
    ringCandidates.map(candidate =>
      if (settledRings.contains(candidate))
        RingSettlementDecision(
        ringHash = candidate.hash,
        decision = SettlementDecision.Settled,
        orders = candidate.orders)
      else
        RingSettlementDecision(
          ringHash = candidate.hash,
          decision = SettlementDecision.UnSettled,
          orders = candidate.orders))
  }
}

