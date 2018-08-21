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

case class GethClientConfig(
  host: String,
  port: Int,
  ssl: Boolean = false)

class EthClientImpl(
  val config: GethClientConfig)(
  implicit
  val system: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val executionContex: ExecutionContextExecutor) extends EthClient {

  private val id = "1"
  private val jsonrpcversion = "2.0"
  private val post = HttpMethods.POST
  private val uri = "http://" + config.host + ":" + config.port.toString + "/"

  def ethGetBalance(req: EthGetBalanceRequest): Future[EthGetBalanceResponse] = {
    val method = "eth_getBalance"
    val params = Seq[String](req.address, req.tag)

    for {
      json <- handleRequest(method, params)
      resp = JsonFormat.parser.fromJsonString[EthGetBalanceResponse](json)
    } yield resp
  }

  def getTransactionByHash(req: GetTransactionByHashRequest): Future[GetTransactionByHashResponse] = {
    val method = "eth_getTransactionByHash"
    val params = Seq[String](req.hash)

    for {
      json <- handleRequest(method, params)
      resp = JsonFormat.parser.fromJsonString[GetTransactionByHashResponse](json)
    } yield resp
  }

  private def handleRequest(method: String, params: Seq[String]): Future[String] = {
    val request = JsonRPCRequest()
      .withId(id)
      .withJsonrpc(jsonrpcversion)
      .withMethod(method)
      .withParams(params)

    val jsonReq = JsonFormat.toJsonString(request)
    val entity = HttpEntity(ContentTypes.`application/json`, jsonReq)
    val httpRequest = HttpRequest.apply(method = post, uri = uri, entity = entity)

    for {
      httpResp <- Http().singleRequest(httpRequest)
      jsonResp <- httpResp.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
    } yield jsonResp
  }
}
