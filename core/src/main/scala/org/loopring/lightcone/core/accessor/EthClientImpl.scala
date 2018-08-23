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
import org.loopring.lightcone.proto.eth_jsonrpc._

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scalapb.json4s.JsonFormat
import spray.json._
import DefaultJsonProtocol._

case class GethClientConfig(
  host: String,
  port: Int,
  ssl: Boolean = false)

case class JsonRpcRequest(id: String, jsonrpc: String, method: String, params: Seq[Any])
case class JsonDebugRequest(id: String, method: String, params: Seq[Any])
case class DebugParams(timeout: String, tracer: String)

class EthClientImpl(
  val config: GethClientConfig)(
  implicit
  val system: ActorSystem) extends EthClient {

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  private val id = "1"
  private val jsonrpcversion = "2.0"
  private val post = HttpMethods.POST
  private val uri = "http://" + config.host + ":" + config.port.toString + "/"
  private val jsonRpcRequestFormater = JsonRequestFormat
  //private val jsonDebugRequestFormater = JsonDebugFormat
  private val debugTimeoutStr = "5s"
  private val debugTracerStr = "callTracer"

  // todo(fukun): 如何解决泛型在json解析时的实例化问题
  def request[R, P](req: R, method: String, params: Seq[Any]): Future[P] = ???
  //  {
  //    for {
  //      json <- handleJsonRpcRequest(method, params)
  //      resp = JsonFormat.parser.fromJsonString[P](json)
  //    } yield resp
  //  }

  def ethGetBalance(req: EthGetBalanceRequest): Future[EthGetBalanceResponse] = {
    val method = "eth_getBalance"
    val params = Seq[Any](req.address, req.tag)

    for {
      json <- handleJsonRpcRequest(method, params)
      resp = JsonFormat.parser.fromJsonString[EthGetBalanceResponse](json)
    } yield resp
  }

  def getTransactionByHash(req: GetTransactionByHashRequest): Future[GetTransactionByHashResponse] = {
    val method = "eth_getTransactionByHash"
    val params = Seq[Any](req.hash)

    for {
      json <- handleJsonRpcRequest(method, params)
      resp = JsonFormat.parser.fromJsonString[GetTransactionByHashResponse](json)
    } yield resp
  }

  def getTransactionReceipt(req: GetTransactionReceiptRequest): Future[GetTransactionReceiptResponse] = {
    val method = "eth_getTransactionReceipt"
    val params = Seq[Any](req.hash)

    for {
      json <- handleJsonRpcRequest(method, params)
      resp = JsonFormat.parser.fromJsonString[GetTransactionReceiptResponse](json)
    } yield resp
  }

  def getBlockWithTxHashByNumber(req: GetBlockWithTxHashByNumberRequest): Future[GetBlockWithTxHashByNumberResponse] = {
    val method = "eth_getBlockByNumber"
    val params = Seq[Any](req.blockNumber, false)

    for {
      json <- handleJsonRpcRequest(method, params)
      resp = JsonFormat.parser.fromJsonString[GetBlockWithTxHashByNumberResponse](json)
    } yield resp
  }

  def getBlockWithTxObjectByNumber(req: GetBlockWithTxObjectByNumberRequest): Future[GetBlockWithTxObjectByNumberResponse] = {
    val method = "eth_getBlockByNumber"
    val params = Seq[Any](req.blockNumber, true)

    for {
      json <- handleJsonRpcRequest(method, params)
      resp = JsonFormat.parser.fromJsonString[GetBlockWithTxObjectByNumberResponse](json)
    } yield resp
  }

  def getBlockWithTxHashByHash(req: GetBlockWithTxHashByHashRequest): Future[GetBlockWithTxHashByHashResponse] = {
    val method = "eth_getBlockByHash"
    val params = Seq[Any](req.blockHash, false)

    for {
      json <- handleJsonRpcRequest(method, params)
      resp = JsonFormat.parser.fromJsonString[GetBlockWithTxHashByHashResponse](json)
    } yield resp
  }

  def getBlockWithTxObjectByHash(req: GetBlockWithTxObjectByHashRequest): Future[GetBlockWithTxObjectByHashResponse] = {
    val method = "eth_getBlockByHash"
    val params = Seq[Any](req.blockHash, true)

    for {
      json <- handleJsonRpcRequest(method, params)
      resp = JsonFormat.parser.fromJsonString[GetBlockWithTxObjectByHashResponse](json)
    } yield resp
  }

  def traceTransaction(req: TraceTransactionRequest): Future[TraceTransactionResponse] = {
    val method = "debug_traceTransaction"
    val debugParams = DebugParams(debugTimeoutStr, debugTracerStr)
    val params = Seq[Any](req.txhash, debugParams)

    for {
      json <- handleJsonRpcRequest(method, params)
      resp = JsonFormat.parser.fromJsonString[TraceTransactionResponse](json)
    } yield resp
  }

  private def handleJsonRpcRequest(method: String, params: Seq[Any]): Future[String] = {
    val request = JsonRpcRequest(id, jsonrpcversion, method, params)
    val jsonReq = formatJsonRpcRequest(request)
    val entity = HttpEntity(ContentTypes.`application/json`, jsonReq.toString())
    handleRequest(entity)
  }

  //  private def handleJsonDebugRequest(method: String, params: Seq[Any]): Future[String] = {
  //    val request = JsonDebugRequest(id, method, params)
  //    val jsonReq = formatJsonDebugRequest(request)
  //    val entity = HttpEntity(ContentTypes.`application/json`, jsonReq.toString())
  //    handleRequest(entity)
  //  }

  private def handleRequest(entity: RequestEntity): Future[String] = {
    val httpRequest = HttpRequest.apply(method = post, uri = uri, entity = entity)

    for {
      httpResp <- Http().singleRequest(httpRequest)
      jsonResp <- httpResp.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
    } yield jsonResp
  }

  ////////////////////////////////////////////////////////////////////
  //
  // JsonRpcRequest format and parse
  //
  ////////////////////////////////////////////////////////////////////
  implicit object JsonRequestFormat extends JsonFormat[JsonRpcRequest] {
    override def write(request: JsonRpcRequest): JsValue = JsObject(Map(
      "id" -> JsString(request.id),
      "jsonrpc" -> JsString(request.jsonrpc),
      "method" -> JsString(request.method),
      "params" -> JsArray(request.params.map(x => writeAny(x)): _*)))

    override def read(value: JsValue): JsonRpcRequest = {
      value.asJsObject.getFields("id", "jsonrpc", "method", "params") match {
        case Seq(JsString(id), JsString(jsonrpc), JsString(method), JsArray(params)) =>
          JsonRpcRequest(id, jsonrpc, method, params)
        case _ => throw new Exception("JsonRpcRequest expected")
      }
    }
  }

  //  implicit object JsonDebugFormat extends JsonFormat[JsonDebugRequest] {
  //    override def write(request: JsonDebugRequest): JsValue = JsObject(Map(
  //      "id" -> JsString(request.id),
  //      "method" -> JsString(request.method),
  //      "params" -> JsArray(request.params.map(x => writeAny(x)): _*)))
  //
  //    override def read(value: JsValue): JsonDebugRequest = {
  //      value.asJsObject.getFields("id", "method", "params") match {
  //        case Seq(JsString(id), JsString(method), JsArray(params)) =>
  //          JsonDebugRequest(id, method, params)
  //        case _ => throw new Exception("JsonDebugRequest expected")
  //      }
  //    }
  //  }

  private def writeAny(src: Any) = src match {
    case n: Int => JsNumber(n)
    case s: String => JsString(s)
    case b: Boolean if b.equals(true) => JsTrue
    case b: Boolean if b.equals(false) => JsFalse
    case o: DebugParams => JsObject(Map(
      "timeout" -> JsString(o.timeout),
      "tracer" -> JsString(o.tracer)))
    case _ => JsNull
  }

  private def readAny(value: JsValue) = value match {
    case JsNumber(n) => n.intValue()
    case JsString(s) => s
    case JsTrue => true
    case JsFalse => false
    case JsObject(o) => value.asJsObject.getFields("timeout", "tracer") match {
      case Seq(JsString(timeout), JsString(tracer)) => DebugParams(timeout, tracer)
      case _ => null
    }
    case _ => null
  }

  def formatJsonRpcRequest(req: JsonRpcRequest): JsValue = req.toJson
  def parseJsonRpcRequest(data: JsValue): JsonRpcRequest = data.convertTo[JsonRpcRequest]
  //  def formatJsonDebugRequest(req: JsonDebugRequest): JsValue = req.toJson
  //  def parseJsonDebugRequest(data: JsValue): JsonDebugRequest = data.convertTo[JsonDebugRequest]
}
