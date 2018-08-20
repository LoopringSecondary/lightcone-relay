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
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.{ ByteString, Timeout }
import org.loopring.lightcone.data.eth_jsonrpc.{ JsonRPCRequest, JsonRPCResponse }
import org.scalatest.FlatSpec

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class ethGetBalanceSpec extends FlatSpec {

  info("execute cmd [sbt lib/\"testOnly *ethGetBalanceSpec\"] to test single spec of eth_getBalance")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val req: JsonRPCRequest = JsonRPCRequest().withId("1").withJson(
    """
    {
 |      "jsonrpc": "2.0"
 |      , "method": "eth_getBalance"
 |      , "params":
 |      ["0x4bad3053d574cd54513babe21db3f09bea1d387d"
 |      , "latest"
 |      ], "id": 1
 |    }
    """.stripMargin)

  val request = HttpRequest.apply(
    method = HttpMethods.POST,
    uri = "http://127.0.0.1:8545/",
    entity = HttpEntity(ContentTypes.`application/json`, ByteString(req.json)))

  "origin response" should "be a json stri" in {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

    val result = Await.result(responseFuture, 1 seconds)
    info(result.entity.withContentType(ContentTypes.`application/json`).toString)
  }

  "json support response" should "be a entity" in {
    implicit val timeout = Timeout(5 seconds)

    val respFuture = for {
      httpResp <- Http().singleRequest(request)
      jsonResp <- httpResp.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
    } yield JsonRPCResponse(id = req.id, json = jsonResp)

    val result = Await.result(respFuture, timeout.duration)
    info(s"geth http client get resp: id->${result.id}, json->${result.json}")
  }

}

