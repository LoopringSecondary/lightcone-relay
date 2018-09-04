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
import org.loopring.lightcone.core.utils.{ RingCandidate, RingEvaluator, RingSubmitter }
import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order.OrderSettleAmount
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
  val lrcAddress = "0xef68e7c694f40c8202821edf525de3782458639f"
  var marketIds = Seq[String]()
  var miner = RingSubmitter(ethClient)
  var evaluator = RingEvaluator(lrcAddress = lrcAddress)
  var finders = Map[String, ActorRef]()

  override def receive: Receive = super.receive orElse {
    case settings: RingMinerSettings =>
      marketIds = settings.marketIds
      miner = miner.copy(
        contract = settings.contract,
        chainId = settings.chainId.toByte,
        keystorePwd = settings.keystorePwd,
        keystoreFile = settings.keystoreFile)
      evaluator = evaluator.copy(miner = miner.credentials.getAddress)
      if (settings.gasUsedOfOrders.nonEmpty) {
        evaluator = evaluator.copy(gasUsedOfOrders = settings.gasUsedOfOrders)
      }
      if (settings.walletSplit.signum >= 0) {
        evaluator = evaluator.copy(walletSplit = Rational(settings.walletSplit))
      }
      finders = marketIds.map { id => (id, Routers.ringFinder(id)) }.toMap[String, ActorRef]
      initAndStartNextRound(settings.scheduleDelay)
  }

  def handleRepeatedJob() = for {
    resps <- Future.sequence(finders.map { finder =>
      for {
        resp <- finder._2 ? GetRingCandidates()
      } yield (finder._1, resp)
    }).mapTo[Seq[(String, RingCandidates)]]
    ringToFinderMap = resps.flatMap(resp => resp._2.rings.map(ring => (ring.hash, resp._1))).toMap
    ringCandidates = RingCandidates(resps.flatMap(_._2.rings))
    ringsToSettle <- evaluator.getRingCadidatesToSettle(ringCandidates)
  } yield {
    decideRingCandidates(ringCandidates.rings, ringsToSettle)
      .groupBy { decision =>
        ringToFinderMap(decision.ringHash)
      }.foreach { decision =>
        val finder = Routers.ringFinder(decision._1)
        finder ! NotifyRingSettlementDecisions(decision._2)
      }
    for (cadidate <- ringsToSettle) {
      miner.signAndSendTx(cadidate)
    }
  }

  def decideRingCandidates(ringCandidates: Seq[Ring], settledRings: Seq[RingCandidate]): Seq[RingSettlementDecision] = {
    val settledRingHashSet = settledRings.map(_.rawRing.hash).toSet
    ringCandidates.map(candidate =>
      if (settledRingHashSet.contains(candidate.hash))
        RingSettlementDecision(
        ringHash = candidate.hash,
        decision = SettlementDecision.Settled,
        ordersSettleAmount = candidate.orders.map(o =>
          OrderSettleAmount(orderHash = o.rawOrder.get.hash, amount = o.dealtAmountS)))
      else
        RingSettlementDecision(
          ringHash = candidate.hash,
          decision = SettlementDecision.UnSettled,
          ordersSettleAmount = candidate.orders.map(o =>
            OrderSettleAmount(orderHash = o.rawOrder.get.hash, amount = o.dealtAmountS))))
  }
}

