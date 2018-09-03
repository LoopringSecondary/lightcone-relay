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
import org.loopring.lightcone.core.conveter.{ RingConverter, RingMinedConverter, TransferEventConverter }
import org.loopring.lightcone.proto.token._
import org.loopring.lightcone.proto.block_chain_event._
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.solidity._
import org.loopring.lightcone.lib.abi.AbiSupporter
import org.loopring.lightcone.proto.common.StartNewRound
import com.google.inject._

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
  val tokenList: TokenList,
  val accessor: EthClient,
  val abiSupporter: AbiSupporter,
  val ringConverter: RingConverter,
  val ringMinedConverter: RingMinedConverter,
  val transferEventConverter: TransferEventConverter) extends RepeatedJobActor {

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
  val supportedContracts: Seq[String] = tokenList.list.map(x => safeAddress(x.protocol))

  var currentBlockNumber: BigInt = BigInt(0)

  def handleForkEvent(): Future[Seq[Any]] = for {
    forkevt <- getForkBlock()
  } yield forkevt match {
    case f: ChainRolledBack if f.fork.equals(true) => Seq(f)
    case _ => Seq()
  }

  def handleUnforkEvent(): Future[Seq[Any]] = for {
    _ <- Future {}

    block = safeBlockHex(currentBlockNumber)
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
  } yield ChainRolledBack().withFork(false)

  // todo
  def unpackMinedTransaction(tx: MinedTransaction): Seq[Any] = {
    //unpackSingleInput(tx.trace.input)
    tx.receipt.logs.map(unpackSingleEvent(_))
    //++ tx.trace.calls.map(x => decode(x.input)).seq
  }

  def unpackPendingTransaction(tx: Transaction): Seq[Any] = ???

  // todo 这里需要整合所有的数据到一个transaction里面,对于callArgs&transaction&receipt统一处理
  def unpackSingleInput(input: String): Seq[Any] = {
    val sig = abiSupporter.findTransactionFunctionSig(input)
    if (abiSupporter.isSupportedFunctionSig(sig)) {
      val decodedinput = abiSupporter.getInputBytes(input)
      val abi = abiSupporter.findFunctionWithSig(sig)
      val decodedseq = abi.decode(decodedinput).toArray().toSeq

      abi.name match {
        case abiSupporter.FN_SUBMIT_RING =>
          ringConverter.convert(decodedseq)
      }
    } else {
      Seq()
    }
  }

  def unpackSingleEvent(log: Log): Seq[Any] = {
    require(log.topics.length > 0)
    val sig = abiSupporter.findReceiptEventSig(log.topics.head)
    if (abiSupporter.isSupportedEventSig(sig)) {
      val decodeddata = abiSupporter.getLogDataBytes(log.data)
      val decodedtopics = log.topics.map(x => abiSupporter.getLogDataBytes(x)).toArray
      val abi = abiSupporter.findEventWithSig(sig)
      val decodedseq = abi.decode(decodeddata, decodedtopics).toArray().toSeq

      abi.name match {
        case abiSupporter.EV_RING_MINED =>
          ringMinedConverter.convert(decodedseq)
        case abiSupporter.EV_TRANSFER =>
          transferEventConverter.convert(decodedseq)
        case _ => Seq()
      }
    } else {
      Seq()
    }
  }

  def route(onchainEvent: Any) = onchainEvent match {
    case balance: SubmitRingFunction =>
    case ring: RingDetectedInMemPoll =>
    case ringMined: RingMined =>
    case cancel: OrderCancelled =>
    case cutoff: Cutoff =>
    case cutoffPair: CutoffPair =>
  }

  // todo: 使用Hex.toHexString会导致多出一些0,而现有的方式为转为int后toHexString
  private def safeBlockHex(blockNumber: BigInt): String = {
    "0x" + blockNumber.intValue().toHexString
  }

  // todo: 首次从数据库获取blockNumber,后续启动从数据库获取blockNumber
  private def setCurrentBlock() = for {
    _ <- Future {}
  } yield currentBlockNumber = BigInt.apply(43206)

  private def isProtocolSupported(txTo: String): Boolean = supportedContracts.contains(safeAddress(txTo))

  // todo: other validate
  private def safeAddress(address: String): String = address.toUpperCase()
}
