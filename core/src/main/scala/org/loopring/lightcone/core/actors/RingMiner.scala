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
import org.loopring.lightcone.core.utils._
import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order.OrderSettling
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
    timeout: Timeout
)
  extends RepeatedJobActor {
  val lrcAddress = "0xef68e7c694f40c8202821edf525de3782458639f"

  var marketIds = Seq[String]()
  var submitter: RingSubmitter = null
  var evaluator: RingEvaluator = null
  var finders = Map[String, ActorRef]()

  override def receive: Receive = super.receive orElse {
    case settings: RingMinerSettings ⇒
      marketIds = settings.marketIds
      settings.submitterSettings match {
        case Some(submitterSettings) ⇒
          submitter = new RingSubmitterImpl(
            ethClient = ethClient,
            contract = submitterSettings.contract,
            chainId = submitterSettings.chainId.toByte,
            keystorePwd = submitterSettings.keystorePwd,
            keystoreFile = submitterSettings.keystoreFile
          )
        case None ⇒
      }
      settings.evaluatorSettings match {
        case Some(evaluatorSettings) ⇒
          evaluator = new RingEvaluatorImpl(
            submitterAddress = submitter.getSubmitterAddress(),
            lrcAddress = lrcAddress,
            gasUsedOfOrders = evaluatorSettings.gasUsedOfOrders,
            walletSplit = Rational(evaluatorSettings.walletSplit)
          )
        case None ⇒
      }
      finders = marketIds.map { id ⇒ (id, Routers.ringFinder(id)) }.toMap[String, ActorRef]
      initAndStartNextRound(settings.scheduleDelay)
  }

  def handleRepeatedJob() = for {
    //finderid以及其返回的RingCandidates
    resps ← Future.sequence(finders.map {
      case (finderId, finder) ⇒
        for {
          resp ← finder ? GetRingCandidates()
        } yield (finderId, resp)
    }).mapTo[Seq[(String, RingCandidates)]]
    //记录ring与finderid的对应关系
    ringToFinderMap = resps.flatMap(resp ⇒ resp._2.rings.map(ring ⇒ (ring.hash, resp._1))).toMap
    ringCandidates = RingCandidates(resps.flatMap(_._2.rings))
    ringsToSettle ← evaluator.getRingCadidatesToSettle(ringCandidates)
  } yield {
    decideRingCandidates(ringCandidates.rings, ringsToSettle)
      .groupBy { decision ⇒
        ringToFinderMap(decision.ringHash) //按照finderid分开，将对应的decision返回给各自的finder
      } foreach {
        case (finderId, settlementDecision) ⇒
          val finder = Routers.ringFinder(finderId)
          finder ! NotifyRingSettlementDecisions(settlementDecision)
      }
    ringsToSettle.foreach(submitter.signAndSendTx)
  }

  def decideRingCandidates(
    ringCandidates: Seq[Ring],
    settledRings: Seq[RingCandidate]
  ): Seq[RingSettlementDecision] = {
    val settledRingHashSet = settledRings.map(_.rawRing.hash).toSet

    ringCandidates.map { candidate ⇒
      if (settledRingHashSet.contains(candidate.hash))
        RingSettlementDecision(
          ringHash = candidate.hash,
          decision = SettlementDecision.Settled,
          ordersSettling = candidate.orders.map { o ⇒
            OrderSettling(orderHash = o.rawOrder.get.hash, amount = o.dealtAmountS)
          }
        )
      else
        RingSettlementDecision(
          ringHash = candidate.hash,
          decision = SettlementDecision.UnSettled,
          ordersSettling = candidate.orders.map { o ⇒
            OrderSettling(orderHash = o.rawOrder.get.hash, amount = o.dealtAmountS)
          }
        )
    }
  }
}

