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

case class JsonRpcReq(
  id: Int,
  jsonrpc: String,
  method: String,
  params: Seq[Any])

case class DebugParams(
  timeout: String,
  tracer: String)

//case class CallArgs(from: String, to: String, gas: String, gasPrice: String, value: String, data: String)

class EthClientImpl(
  val config: GethClientConfig,
  val abiStrMap: Map[String, String])(
  implicit
  val system: ActorSystem) extends EthClient {

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val erc20Abi = Abi.fromJson(abiStrMap("erc20"))

  private val id: Int = 1
  private val JSON_RPC_VERSION = "2.0"
  private val DEBUG_TIMEOUT_STR = "5s"
  private val DEBUG_TRACER = "callTracer"
  private val ETH_CALL = "eth_call"
  private val post = HttpMethods.POST
  private val uri = s"http://${config.host}:${config.port.toString}"
  implicit val jsonRpcReqFormater = JsonReqFormat

  // def request[R, P](req: R, method: String, params: Seq[Any]): Future[P] = ???

  // eth actions
  def ethGetBalance(req: EthGetBalanceReq) =
    httpGet[EthGetBalanceRes]("eth_getBalance") {
      Seq[Any](req.address, req.tag)
    }

  def getTransactionByHash(req: GetTransactionByHashReq) =
    httpGet[GetTransactionByHashRes]("eth_getTransactionByHash") {
      Seq[Any](req.hash)
    }

  def getTransactionReceipt(req: GetTransactionReceiptReq) =
    httpGet[GetTransactionReceiptRes]("eth_getBlockByNumber") {
      Seq[Any](req.hash)
    }

  def getBlockWithTxHashByNumber(req: GetBlockWithTxHashByNumberReq) =
    httpGet[GetBlockWithTxHashByNumberRes]("eth_getBlockByNumber") {
      Seq[Any](req.blockNumber, true)
    }

  def getBlockWithTxObjectByNumber(req: GetBlockWithTxObjectByNumberReq) =
    httpGet[GetBlockWithTxObjectByNumberRes]("eth_getBlockByHash") {
      Seq[Any](req.blockNumber, true)
    }

  def getBlockWithTxHashByHash(req: GetBlockWithTxHashByHashReq) =
    httpGet[GetBlockWithTxHashByHashRes]("eth_getBlockByHash") {
      Seq[Any](req.blockHash, true)
    }

  def getBlockWithTxObjectByHash(req: GetBlockWithTxObjectByHashReq) =
    httpGet[GetBlockWithTxObjectByHashRes]("eth_getBlockByHash") {
      Seq[Any](req.blockHash, true)
    }

  def traceTransaction(req: TraceTransactionReq) =
    httpGet[TraceTransactionRes]("debug_traceTransaction") {
      val debugParams = DebugParams(DEBUG_TIMEOUT_STR, DEBUG_TRACER)
      Seq[Any](req.txhash, debugParams)
    }

  // erc20 contract requests
  def getBalance(req: GetBalanceReq) =
    httpGet[GetBalanceRes](ETH_CALL) {
      val function = findErc20Function("balanceOf")
      val data = bytesToHex(function.encode(req.owner))
      val args = CallArgs().withTo(req.token).withData(data)
      Seq[Any](args, req.tag)
    }

  def getAllowance(req: GetAllowanceReq) =
    httpGet[GetAllowanceRes](ETH_CALL) {
      val function = findErc20Function("balanceOf")
      val data = bytesToHex(function.encode(req.owner))
      val args = CallArgs().withTo(req.token).withData(data)
      Seq[Any](args, req.tag)
    }

  private def httpGet[R <: scalapb.GeneratedMessage with scalapb.Message[R]](
    method: String)(
    params: Seq[Any])(
    implicit
    c: scalapb.GeneratedMessageCompanion[R]): Future[R] = {
    val request = JsonRpcReq(id, JSON_RPC_VERSION, method, params)
    val jsonReq = formatJsonRpcReq(request)
    val entity = HttpEntity(ContentTypes.`application/json`, jsonReq.toString())
    val httpReq = HttpRequest.apply(method = post, uri = uri, entity = entity)

    for {
      httpRes <- Http().singleRequest(httpReq)
      jsonStr <- httpRes.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
      resp = JsonFormat.parser.fromJsonString[R](jsonStr)
    } yield resp
  }

  private def findErc20Function(name: String) = {
    val method: Predicate[Abi.Function] = (x) => x.name.equals(name)
    erc20Abi.findFunction(method)
  }

  private def bytesToHex(data: Array[Byte]): String = "0x" + Hex.toHexString(data)

  def formatJsonRpcReq(req: JsonRpcReq): JsValue = req.toJson
  def parseJsonRpcReq(data: JsValue): JsonRpcReq = data.convertTo[JsonRpcReq]
}