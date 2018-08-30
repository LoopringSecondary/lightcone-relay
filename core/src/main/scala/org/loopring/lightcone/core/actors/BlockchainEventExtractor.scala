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

import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.core.actors.base.RepeatedJobActor
import org.loopring.lightcone.proto.token._
import org.loopring.lightcone.proto.block_chain_event._
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.solidity._
import org.loopring.lightcone.core.etypes._
import org.loopring.lightcone.lib.abi.AbiSupport
import org.loopring.lightcone.proto.common.StartNewRound

import org.spongycastle.util.encoders.Hex

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BlockchainEventExtractor
  extends base.Deployable[BlockchainEventExtractorSettings] {
  val name = "block_event_extractor"
  override val isSingleton = true

  def getCommon(s: BlockchainEventExtractorSettings) =
    base.CommonSettings(None, s.roles, 1)
}

class BlockchainEventExtractor()(implicit
  val tokenList: Seq[Token],
  val accessor: EthClient) extends RepeatedJobActor with AbiSupport {

  var settingsOpt: Option[BlockchainEventExtractorSettings] = None

  override def receive: Receive = {
    case settings: BlockchainEventExtractorSettings =>
      settingsOpt = Some(settings)
      initAndStartNextRound(settings.scheduleDelay)
    case newRound: StartNewRound =>
      handleRepeatedJob()
    case _ => throw new Exception("message invalid")
  }

  // 每次处理一个块,
  // 从block里获取对应的transaction hash
  // 获取transactionReceipt以及对应的trace数据
  // 解析后将数据打包成多个完整的transaction发送出去
  // todo: find roll back
  override def handleRepeatedJob() = for {
    _ <- setCurrentBlock()
    forkseq <- handleForkEvent()
  } yield if (forkseq.size > 0)
    forkseq.map(route(_))
  else {
    for { list <- handleUnforkEvent() } yield list.map(route(_))
  }

  // todo: get protocol address(delegate, impl, token register...) on chain
  val supportedContracts: Seq[String] = tokenList.map(x => safeAddress(x.protocol))

  var currentBlockNumber: BigInt = BigInt(0)

  // todo: validate fork
  def handleForkEvent(): Future[Seq[Any]] = for {
    forkevt <- getForkBlock()
  } yield forkevt match {
    case f: ChainRolledBack if f.delectedBlockHash.toString().nonEmpty => Seq(f)
    case _ => Seq()
  }

  def handleUnforkEvent(): Future[Seq[Any]] = for {
    _ <- Future { println(s"-----current block is ${currentBlockNumber.toString()}") }
    block = "0x" + Hex.toHexString(currentBlockNumber.toByteArray)
    _ <- Future { println(s"---- block hex is ${block}") }
    blockReq = GetBlockWithTxHashByNumberReq(block)
    block <- accessor.getBlockWithTxHashByNumber(blockReq)

    minedTxs <- getMinedTransactions(block.getResult.transactions)
    minedSeq = minedTxs.map(unpackMinedTransaction(_))

    pendingTxs <- getPendingTransactions(block.getResult.transactions)
    pendingSeq = pendingTxs.map(unpackPendingTransaction(_))

    list = minedSeq ++ pendingSeq
  } yield list

  def getMinedTransactions(hashseq: Seq[String]): Future[Seq[MinedTransaction]] = Future.sequence(
    hashseq.map(txhash =>
      for {
        receipt <- accessor.getTransactionReceipt(GetTransactionReceiptReq(txhash))
        trace <- accessor.traceTransaction(TraceTransactionReq(txhash))
      } yield MinedTransaction(receipt.getResult, trace.getResult)))

  // todo
  def getPendingTransactions(hashSeq: Seq[String]): Future[Seq[Transaction]] = for {
    _ <- Future {}
  } yield Seq()

  // todo
  def getForkBlock(): Future[Any] = for {
    _ <- Future {}
  } yield ChainRolledBack()

  // todo: 解析receipt, trace等并转换成blockChainEvent
  def unpackMinedTransaction(tx: MinedTransaction): Seq[Any] = ???

  def unpackPendingTransaction(tx: Transaction): Seq[Any] = ???

  def route(onchainEvent: Any) = onchainEvent match {
    case balance: SubmitRingFunction =>
    case ring: RingDetectedInMemPoll =>
    case ringMined: RingMined =>
    case cancel: OrderCancelled =>
    case cutoff: Cutoff =>
    case cutoffPair: CutoffPair =>
  }

  // todo: 首次从数据库获取blockNumber,后续启动从数据库获取blockNumber
  private def setCurrentBlock() = for {
    _ <- Future {}
  } yield currentBlockNumber = BigInt.apply(43206)

  private def isProtocolSupported(txTo: String): Boolean = supportedContracts.contains(safeAddress(txTo))

  // todo: other validate
  private def safeAddress(address: String): String = address.toUpperCase()
}
