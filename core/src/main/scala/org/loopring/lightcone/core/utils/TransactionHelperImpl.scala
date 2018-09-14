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
import org.loopring.lightcone.lib.abi._
import org.loopring.lightcone.proto.block_chain_event._
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.proto.token.TokenList

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TransactionHelperImpl @Inject() (
    val tokenList: TokenList,
    val accessor: EthClient,
    val erc20Abi: Erc20Abi,
    val wethAbi: WethAbi,
    val loopringAbi: LoopringAbi
)
  extends TransactionHelper {

  // todo: get protocol address(delegate, impl, token register...) on chain
  val supportedContracts: Seq[String] = tokenList.list.map(x ⇒ safeAddress(x.protocol))

  def getMinedTransactions(hashseq: Seq[String]): Future[Seq[FullTransaction]] =
    Future.sequence(hashseq.map(txhash ⇒
      for {
        tx ← accessor.getTransactionByHash(GetTransactionByHashReq(txhash))
        receipt ← accessor.getTransactionReceipt(GetTransactionReceiptReq(txhash))
        trace ← accessor.traceTransaction(TraceTransactionReq(txhash))
      } yield FullTransaction()
        .withTx(tx.getResult)
        .withReceipt(receipt.getResult)
        .withTrace(trace.getResult)))

  // todo
  def getPendingTransactions(hashSeq: Seq[String]): Future[Seq[Transaction]] = for {
    _ ← Future {}
  } yield Seq()

  def unpackMinedTransaction(src: FullTransaction): Seq[Any] = {
    val header = src.getTxHeader
    val mainseq = unpackSingleInput(src.getTx.input, header)

    val callseq = src.trace match {
      case Some(x) ⇒ x.calls.map { n ⇒
        unpackSingleInput(n.input, header.fillWith(x))
      }.reduceLeft((a, b) ⇒ a ++ b)
      case _ ⇒ Seq()
    }

    val evtseq = src.receipt match {
      case Some(x) ⇒ x.logs.map { n ⇒
        unpackSingleEvent(n, header.fillWith(x))
      }.reduceLeft((a, b) ⇒ a ++ b)
      case _ ⇒ Seq()
    }

    mainseq ++ callseq ++ evtseq
  }

  def unpackPendingTransaction(tx: Transaction): Seq[Any] = ???

  def unpackSingleInput(input: String, header: TxHeader): Seq[Any] = {
    erc20Abi.decodeInputAndAssemble(input, header) ++
      wethAbi.decodeInputAndAssemble(input, header) ++
      loopringAbi.decodeInputAndAssemble(input, header)
  }

  def unpackSingleEvent(log: Log, header: TxHeader): Seq[Any] = {
    erc20Abi.decodeLogAndAssemble(log, header) ++
      wethAbi.decodeLogAndAssemble(log, header) ++
      loopringAbi.decodeLogAndAssemble(log, header)
  }

  def isContractAddressSupported(txTo: String) = supportedContracts.contains(safeAddress(txTo))

  private def safeAddress(address: String): String = address.toUpperCase()

}
