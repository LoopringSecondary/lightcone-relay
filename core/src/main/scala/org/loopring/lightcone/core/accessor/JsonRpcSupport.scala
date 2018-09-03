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
import scala.concurrent._
import scalapb.json4s.JsonFormat
import org.json4s._
import org.json4s.native.Serialization

// TODO(fukun): use a connection pool?
trait JsonRpcSupport {
  implicit val system: ActorSystem
  val abi: ContractABI
  val uri: String

  implicit lazy val materializer = ActorMaterializer()
  implicit lazy val executionContext = system.dispatcher
  implicit lazy val erc20Abi = abi.get("erc20")

  val id: Int = 1
  val JSON_RPC_VERSION = "2.0"
  val DEBUG_TIMEOUT_STR = "5s"
  val DEBUG_TRACER = "callTracer"
  val ETH_CALL = "eth_call"

  implicit val formats = Serialization.formats(NoTypeHints)

  case class JsonRpcRequest(
    id: Int,
    jsonrpc: String,
    method: String,
    params: Seq[Any])

  def httpPost[R <: scalapb.GeneratedMessage with scalapb.Message[R]](
    method: String)(
      params: Seq[Any])(
        implicit c: scalapb.GeneratedMessageCompanion[R]): Future[R] = {
    val request = JsonRpcRequest(id, JSON_RPC_VERSION, method, params)
    val jsonReq = Serialization.write(request)
    val entity = HttpEntity(ContentTypes.`application/json`, jsonReq.toString())
    val httpReq = HttpRequest.apply(method = HttpMethods.POST, uri = uri, entity = entity)

    for {
      httpRes <- Http().singleRequest(httpReq)
      jsonStr <- httpRes.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
      resp = JsonFormat.parser.fromJsonString[R](jsonStr)
    } yield resp
  }

  def findErc20Function(name: String) = {
    val method: Predicate[Abi.Function] = (x) => x.name.equals(name)
    erc20Abi.findFunction(method)
  }
}