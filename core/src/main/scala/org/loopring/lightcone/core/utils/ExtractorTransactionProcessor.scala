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

package org.loopring.lightcone.core.utils

import com.google.inject.Inject
import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.core.assemble._
import org.loopring.lightcone.lib.abi.AbiSupporter
import org.loopring.lightcone.proto.block_chain_event.{ FullTransaction, RingMined, AddressBalanceChanged }
import org.loopring.lightcone.proto.eth_jsonrpc.{ GetTransactionReceiptReq, Log, TraceTransactionReq, Transaction }
import org.loopring.lightcone.proto.ring.Ring
import org.loopring.lightcone.proto.token.TokenList

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ExtractorTransactionProcessor {

  def getMinedTransactions(hashseq: Seq[String]): Future[Seq[FullTransaction]]
  def getPendingTransactions(hashSeq: Seq[String]): Future[Seq[Transaction]]
  def unpackMinedTransaction(tx: FullTransaction): Seq[Any]
  def unpackPendingTransaction(tx: Transaction): Seq[Any]
  def unpackSingleInput(input: String): Seq[Any]
  def unpackSingleEvent(log: Log): Seq[Any]
  def isContractAddressSupported(txTo: String): Boolean
}

class ExtractorTransactionProcessorImpl @Inject() (
  val tokenList: TokenList,
  val accessor: EthClient,
  val abiSupporter: AbiSupporter,
  val ringAssembler: Assembler[Ring],
  val ringMinedAssembler: Assembler[RingMined],
  val transferEventAssembler: Assembler[AddressBalanceChanged]) extends ExtractorTransactionProcessor {

  // todo: get protocol address(delegate, impl, token register...) on chain
  val supportedContracts: Seq[String] = tokenList.list.map(x => safeAddress(x.protocol))

  def getMinedTransactions(hashseq: Seq[String]): Future[Seq[FullTransaction]] =
    Future.sequence(hashseq.map(txhash =>
      for {
        receipt <- accessor.getTransactionReceipt(GetTransactionReceiptReq(txhash))
        trace <- accessor.traceTransaction(TraceTransactionReq(txhash))
      } yield FullTransaction().withReceipt(receipt.getResult).withTrace(trace.getResult)))

  // todo
  def getPendingTransactions(hashSeq: Seq[String]): Future[Seq[Transaction]] = for {
    _ <- Future {}
  } yield Seq()

  // todo
  def unpackMinedTransaction(tx: FullTransaction): Seq[Any] = {
    //unpackSingleInput(tx.trace.input)
    tx.getReceipt.logs.map(unpackSingleEvent)
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
          Seq(ringAssembler.convert(decodedseq))
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
          Seq(ringMinedAssembler.convert(decodedseq))
        case abiSupporter.EV_TRANSFER =>
          Seq(transferEventAssembler.convert(decodedseq))
        case _ => Seq()
      }
    } else {
      Seq()
    }
  }

  def isContractAddressSupported(txTo: String): Boolean = supportedContracts.contains(safeAddress(txTo))

  // todo: other validate
  private def safeAddress(address: String): String = address.toUpperCase()

}
