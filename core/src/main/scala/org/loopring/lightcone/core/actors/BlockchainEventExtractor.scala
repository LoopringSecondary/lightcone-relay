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
import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.core.actors.base.RepeatedJobActor
import org.loopring.lightcone.proto.token._
import org.loopring.lightcone.proto.block_chain_event._
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.lib.solidity.Abi
import org.loopring.lightcone.proto.solidity._

import scala.concurrent.Future

object BlockchainEventExtractor
  extends base.Deployable[BlockchainEventExtractorSettings] {
  val name = "block_event_extractor"
  val isSingleton = true

  def props = Props(classOf[BlockchainEventExtractor])

  def getCommon(s: BlockchainEventExtractorSettings) =
    base.CommonSettings("", s.roles, 1)
}

case class MinedTransaction(receipt: TransactionReceipt, trace: TraceTransaction)

class BlockchainEventExtractor(
  val tokenList: Seq[Token],
  val abiStrMap: Map[String, String])(implicit val accessor: EthClient) extends RepeatedJobActor {

  val (abiFunctions: Map[String, Abi.Function], abiEvents: Map[String, Abi.Event]) = {
    var fmap: Map[String, Abi.Function] = Map()
    var emap: Map[String, Abi.Event] = Map()

    val abimap = abiStrMap.map(x => { x._1.toLowerCase() -> Abi.fromJson(x._2) })

    abimap.map(x => x._2.iterator.next() match {
      case f: Abi.Function => fmap += f.encodeSignature().toString.toUpperCase() -> f
      case e: Abi.Event => emap += e.encodeSignature().toString.toUpperCase() -> e
    })

    (fmap, emap)
  }

  // todo: get protocol address(delegate, impl, token register...) on chain
  val supportedContracts: Seq[String] = tokenList.map(x => safeAddress(x.protocol))

  override def receive: Receive = {
    case settings: BlockchainEventExtractorSettings =>
  }

  // 每次处理一个块,
  // 从block里获取对应的transaction hash
  // 获取transactionReceipt以及对应的trace数据
  // 解析后将数据打包成多个完整的transaction发送出去
  override def handleRepeatedJob() = for {
    currentBlockNumber <- getCurrentBlock()
    blockReq = GetBlockWithTxHashByNumberReq(currentBlockNumber)
    block <- accessor.getBlockWithTxHashByNumber(blockReq)
    minedTxs <- getMinedTransactions(block.getResult.transactions)
    _ <- minedTxs.map(handleMinedTransaction(_))
    // handlePendingTransaction()
  } yield None

  def handleMinedTransaction(tx: MinedTransaction) = {
    val list = unpackMinedTransaction(tx)
    list.map(route(_))
  }

  def handlePendingTransaction() = ???

  def getMinedTransactions(hashseq: Seq[String]): Future[Seq[MinedTransaction]] = Future.sequence(
    hashseq.map(txhash =>
    for {
      receipt <- accessor.getTransactionReceipt(GetTransactionReceiptReq(txhash))
      trace <- accessor.traceTransaction(TraceTransactionReq(txhash))
    } yield MinedTransaction(receipt.getResult, trace.getResult)
  ))

  def getPendingTransactions(hashSeq: Seq[String]): Future[Seq[Transaction]] = ???
  
  // todo: 解析receipt, trace等并转换成blockChainEvent
  def unpackMinedTransaction(tx: MinedTransaction): Seq[Any] = ???

  def unpackPendingTransaction(tx: Transaction): Seq[Any] = ???

  def route(onchainEvent: Any) = onchainEvent match {
    case balance: SubmitRingFunction =>
    case ring : RingDetectedInMemPoll =>
    case ringMined: RingMined =>
    case cancel: OrderCancelled =>
    case cutoff: Cutoff =>
    case cutoffPair: CutoffPair =>
  }

  def isSupportedFunction(txTo: String, txInput: String): Boolean = {
    require(isProtocolSupported(txTo))
    val functionId = txInput.substring(0, 4).toUpperCase()
    abiFunctions.contains(functionId)
  }

  def isSupportedEvent(txTo: String, firstTopic: String): Boolean = {
    require(isProtocolSupported(txTo))
    val eventId = getEventId(firstTopic)
    abiEvents.contains(eventId)
  }

// todo: 首次从数据库获取blockNumber,后续启动从数据库获取blockNumber
  private def  getCurrentBlock(): Future[String] = for {
    _ <- Future{}
  } yield "0x32"

  private def isProtocolSupported(txTo: String): Boolean = supportedContracts.contains(safeAddress(txTo))

  private def getFunctionId(txInput: String): String = txInput.substring(0, 4).toUpperCase
  private def getEventId(firstTopic: String): String = firstTopic.toUpperCase()

  // todo: other validate
  private def safeAddress(address: String): String = address.toUpperCase()
}
