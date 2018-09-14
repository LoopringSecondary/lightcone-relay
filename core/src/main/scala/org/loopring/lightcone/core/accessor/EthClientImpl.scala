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

package org.loopring.lightcone.core.accessor

import akka.actor._
import com.google.inject._
import com.google.inject.name._
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.lib.abi.{ Erc20Abi, LoopringAbi }
import org.spongycastle.util.encoders.Hex

class EthClientImpl @Inject() (
    val erc20Abi: Erc20Abi,
    val loopringAbi: LoopringAbi,
    val ethereumClientFlow: HttpFlow,
    @Named("ethereum_conn_queuesize") val queueSize: Int
)(
    implicit
    val system: ActorSystem
) extends EthClient with JsonRpcSupport {

  case class DebugParams(
      timeout: String,
      tracer: String
  )

  def ethGetBlockNumber() =
    httpPost[EthGetBlockNumberRes]("eth_blockNumber") {
      Seq()
    }

  def ethGetBalance(req: EthGetBalanceReq) =
    httpPost[EthGetBalanceRes]("eth_getBalance") {
      Seq(req.address, req.tag)
    }

  def getTransactionByHash(req: GetTransactionByHashReq) =
    httpPost[GetTransactionByHashRes]("eth_getTransactionByHash") {
      Seq(req.hash)
    }

  def getTransactionReceipt(req: GetTransactionReceiptReq) =
    httpPost[GetTransactionReceiptRes]("eth_getTransactionReceipt") {
      Seq(req.hash)
    }

  def getBlockWithTxHashByNumber(req: GetBlockWithTxHashByNumberReq) =
    httpPost[GetBlockWithTxHashByNumberRes]("eth_getBlockByNumber") {
      Seq(req.blockNumber, false)
    }

  def getBlockWithTxObjectByNumber(req: GetBlockWithTxObjectByNumberReq) =
    httpPost[GetBlockWithTxObjectByNumberRes]("eth_getBlockByHash") {
      Seq(req.blockNumber, true)
    }

  def getBlockWithTxHashByHash(req: GetBlockWithTxHashByHashReq) =
    httpPost[GetBlockWithTxHashByHashRes]("eth_getBlockByHash") {
      Seq(req.blockHash, false)
    }

  def getBlockWithTxObjectByHash(req: GetBlockWithTxObjectByHashReq) =
    httpPost[GetBlockWithTxObjectByHashRes]("eth_getBlockByHash") {
      Seq(req.blockHash, true)
    }

  def traceTransaction(req: TraceTransactionReq) =
    httpPost[TraceTransactionRes]("debug_traceTransaction") {
      val debugParams = DebugParams(DEBUG_TIMEOUT_STR, DEBUG_TRACER)
      Seq(req.txhash, debugParams)
    }

  def getBalance(req: GetBalanceReq) =
    httpPost[GetBalanceRes](ETH_CALL) {
      val function = erc20Abi.balanceOf
      val data = bytesToHex(function.encode(req.owner))
      val args = TransactionParam().withTo(req.token).withData(data)
      Seq(args, req.tag)
    }

  def getAllowance(req: GetAllowanceReq) =
    httpPost[GetAllowanceRes](ETH_CALL) {
      val function = erc20Abi.allowance
      val data = bytesToHex(function.encode(req.owner))
      val args = TransactionParam().withTo(req.token).withData(data)
      Seq(args, req.tag)
    }

  def sendRawTransaction(req: SendRawTransactionReq) =
    httpPost[SendRawTransactionRes]("eth_sendRawTransaction") {
      Seq(req.data)
    }
  // def getEstimatedGas() = ???

  // def request[R, P](req: R, method: String, params: Seq[Any]): Future[P] = ???

  def bytesToHex(data: Array[Byte]): String = "0x" + Hex.toHexString(data)

}
