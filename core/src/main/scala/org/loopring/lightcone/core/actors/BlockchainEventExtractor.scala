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

import org.loopring.lightcone.core.actors.base.RepeatedJobActor
import org.loopring.lightcone.core.utils.{ BlockHelper, TransactionHelper }
import org.loopring.lightcone.proto.block_chain_event._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.solidity._
import org.loopring.lightcone.proto.common.StartNewRound

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BlockchainEventExtractor
  extends base.Deployable[BlockchainEventExtractorSettings] {
  val name = "block_event_extractor"
  override val isSingleton = true

  def getCommon(s: BlockchainEventExtractorSettings) =
    base.CommonSettings(None, s.roles, 1)
}

class BlockchainEventExtractor(
  settings: BlockchainEventExtractorSettings)(implicit
  val blockHelper: BlockHelper,
  val txHelper: TransactionHelper) extends RepeatedJobActor {

  override def receive: Receive = {
    case settings: BlockchainEventExtractorSettings =>
      initAndStartNextRound(settings.scheduleDelay)
    case newRound: StartNewRound =>
      handleRepeatedJob()
    case _ => throw new Exception("message invalid")
  }

  override def handleRepeatedJob(): Future[Unit] = for {
    forkevt <- blockHelper.getForkEvent()
    list <- if (forkevt.fork.equals(true)) {
      Future(Seq(forkevt))
    } else {
      handleBlock()
    }
  } yield list.map(route)

  def handleBlock(): Future[Seq[Any]] = for {
    block <- blockHelper.getBlock()
    minedTxs <- txHelper.getMinedTransactions(block.transactions)
    minedSeq = minedTxs.map(txHelper.unpackMinedTransaction)

    pendingTxs <- txHelper.getPendingTransactions(block.transactions)
    pendingSeq = pendingTxs.map(txHelper.unpackPendingTransaction)

    list = minedSeq ++ pendingSeq
  } yield list

  // todo
  def route(onchainEvent: Any) = onchainEvent match {
    case balance: SubmitRingFunction =>
    case ring: RingDetectedInMemPoll =>
    case ringMined: RingMined =>
    case cancel: OrderCancelled =>
    case cutoff: Cutoff =>
    case cutoffPair: CutoffPair =>
  }

}
