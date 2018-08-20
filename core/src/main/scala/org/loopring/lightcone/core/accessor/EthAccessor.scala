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
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, HttpMethods, HttpEntity, ContentTypes }
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Flow
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Source, SourceQueueWithComplete, Sink, Keep }
import akka.stream.{ OverflowStrategy, QueueOfferResult }
import akka.util.ByteString

import scala.concurrent.{ Future, Promise }
import scala.util.{ Success, Failure, Try }

case class GethClientConfig(host: String, port: Int, ssl: Boolean = false, ipcPath: String)
case class JsonRPCRequest{}
case class JsonRPCResponse{}

class SimpleGethClientImpl(
                            val system: ActorSystem,
                            val materilizer: ActorMaterializer,
                            val eth: GethClientConfig) extends EthClient {

  def ethGetBalance(address: String): BigInt = {
    //    val req = JsonRPCRequest().withId("1").withJson()
    //    handleRequest(req)
    return null
  }

    def handleRequest(req: JsonRPCRequest): Future[JsonRPCResponse] = {
      val httpReq = HttpRequest(
        method = HttpMethods.POST,
        entity = HttpEntity(ContentTypes.`application/json`, ByteString(req.json)))
      for {
        httpResp ← request(httpReq)
        jsonResp ← httpResp.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
        _ = println(s"geth http client json response => ${jsonResp}")
      } yield JsonRPCResponse(id = req.id, json = jsonResp)
    }

    private def request(request: HttpRequest): Future[HttpResponse] = {
      val responsePromise = Promise[HttpResponse]()
      queue.offer(request -> responsePromise).flatMap {
        case QueueOfferResult.Enqueued ⇒
          responsePromise.future
        case QueueOfferResult.Dropped ⇒
          Future.failed(new RuntimeException("Queue overflowed. Try again later."))
        case QueueOfferResult.Failure(ex) ⇒
          Future.failed(ex)
        case QueueOfferResult.QueueClosed ⇒
          Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
      }
    }

    private val queue: SourceQueueWithComplete[(HttpRequest, Promise[HttpResponse])] =
      Source.queue[(HttpRequest, Promise[HttpResponse])](100, OverflowStrategy.backpressure)
        .via(poolClientFlow)
        .toMat(Sink.foreach({
          case (Success(resp), p) ⇒ p.success(resp)
          case (Failure(e), p) ⇒ p.failure(e) // 这里可以定制一个json
        }))(Keep.left).run()(materilizer)

    private val poolClientFlow: Flow[(HttpRequest, Promise[HttpResponse]), (Try[HttpResponse], Promise[HttpResponse]), Http.HostConnectionPool] = {
      eth.ssl match {
        case true ⇒ Http()(system).cachedHostConnectionPoolHttps[Promise[HttpResponse]](host = eth.host, port = eth.port)
        case _ ⇒ Http()(system).cachedHostConnectionPool[Promise[HttpResponse]](host = eth.host, port = eth.port)
      }
    }
}
