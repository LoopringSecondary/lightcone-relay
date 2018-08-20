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

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpMethods, HttpRequest }
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.loopring.lightcone.data.eth_jsonrpc._

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scalapb.json4s.JsonFormat

case class GethClientConfig(host: String, port: Int, ssl: Boolean = false)

trait EthClient {
  def ethGetBalance(address: String, tag: String): Future[BigInt]
}

class SimpleGethClientImpl(
  val config: GethClientConfig,
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val executionContex: ExecutionContextExecutor) extends EthClient {

  private val id = "1"
  private val jsonrpcversion = "2.0"

  def ethGetBalance(address: String, tag: String): Future[BigInt] = {
    val method = "eth_getBalance"
    val params = Seq[String](address, tag)
    for {
      resp <- handleRequest(method, params)
    } yield hex2int(resp.result)
  }

  private def handleRequest(method: String, params: Seq[String]): Future[JsonRPCResponse] = {
    val request = JsonRPCRequest().withId(id).withJsonrpc(jsonrpcversion).withMethod(method).withParams(params)
    val jsonReq = JsonFormat.toJsonString(request)

    for {
      _ <- Future {}
      httpRequest = HttpRequest.apply(method = HttpMethods.POST, uri = formatUrl, entity = HttpEntity(ContentTypes.`application/json`, ByteString(jsonReq)))
      httpResp <- Http().singleRequest(httpRequest)
      jsonResp <- httpResp.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
      body = JsonFormat.parser.fromJsonString[JsonRPCResponse](jsonResp)
    } yield body
  }

  private val formatUrl: String = {
    "http://" + config.host + ":" + config.port.toString + "/"
  }

  private def hex2int(hex: String): BigInt = {
    if (hex.startsWith("0x")) {
      val subhex = hex.substring(2)
      BigInt(subhex, 16)
    } else {
      BigInt(hex, 16)
    }
  }
}
