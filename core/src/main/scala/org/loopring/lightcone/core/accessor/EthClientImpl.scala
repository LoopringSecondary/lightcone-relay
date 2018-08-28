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
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import org.apache.commons.collections4.Predicate
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.lib.solidity.Abi
import org.spongycastle.util.encoders.Hex

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scalapb.json4s.JsonFormat
import spray.json._
import DefaultJsonProtocol._

case class GethClientConfig(
  host: String,
  port: Int,
  ssl: Boolean = false)

case class DebugParams(
  timeout: String,
  tracer: String)

class EthClientImpl(
  val config: GethClientConfig,
  val abiStrMap: Map[String, String])(
  implicit
  val system: ActorSystem)
  extends EthClient with JsonRpcSupport {

  val uri = s"http://${config.host}:${config.port}"

  // eth actions
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
      val function = findErc20Function("balanceOf")
      val data = bytesToHex(function.encode(req.owner))
      val args = CallArgs().withTo(req.token).withData(data)
      Seq(args, req.tag)
    }

  def getAllowance(req: GetAllowanceReq) =
    httpPost[GetAllowanceRes](ETH_CALL) {
      val function = findErc20Function("balanceOf")
      val data = bytesToHex(function.encode(req.owner))
      val args = CallArgs().withTo(req.token).withData(data)
      Seq(args, req.tag)
    }

  // def getEstimatedGas() = ???

  // def request[R, P](req: R, method: String, params: Seq[Any]): Future[P] = ???

}