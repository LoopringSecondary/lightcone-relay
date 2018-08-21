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
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.scalatest.FlatSpec

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class ethGetBalanceSpec extends FlatSpec {

  info("execute cmd [sbt lib/\"testOnly *ethGetBalanceSpec\"] to test single spec of eth_getBalance")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val timeout = Timeout(5 seconds)

  val config = GethClientConfig.apply(host = "127.0.0.1", port = 8545, ssl = false)
  val req: String =
    """
    {
 |      "jsonrpc": "2.0"
 |      , "method": "eth_getBalance"
 |      , "params":
 |      ["0x4bad3053d574cd54513babe21db3f09bea1d387d"
 |      , "latest"
 |      ], "id": 1
 |    }
    """.stripMargin

  val request = HttpRequest.apply(
    method = HttpMethods.POST,
    uri = "http://127.0.0.1:8545/",
    entity = HttpEntity(ContentTypes.`application/json`, ByteString(req)))

  "origin response" should "be a json stri" in {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

    val result = Await.result(responseFuture, timeout.duration)
    info(result.entity.withContentType(ContentTypes.`application/json`).toString)
  }

  "json support response" should "be a entity" in {
    val respFuture = for {
      httpResp <- Http().singleRequest(request)
      jsonResp <- httpResp.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
    } yield JsonRPCResponse(result = jsonResp)

    val resp = Await.result(respFuture, timeout.duration)
    info(s"geth http client get resp: id->${resp.id}, json->${resp.result}")
  }

  "geth client" should "use accessor" in {
    val geth = new EthClientImpl(config)
    val respFuture = for {
      resp <- geth.ethGetBalance("0x4bad3053d574cd54513babe21db3f09bea1d387d", "latest")
    } yield resp

    val result = Await.result(respFuture, timeout.duration)
    info(s"geth eth_getBalance amount is ${result.toString()}")
  }
}

