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

package org.loopring.ethcube

import scala.util._
import scala.concurrent._
import akka.actor._
import akka.routing._
import akka.stream._
import akka.http.scaladsl.model._
import akka.http.scaladsl._
import akka.stream.scaladsl._
import akka.pattern.pipe
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s._
import org.loopring.ethcube.proto.data._
import scalapb.json4s.JsonFormat

private class HttpConnector(node: EthereumProxySettings.Node)(implicit val materilizer: ActorMaterializer)
  extends Actor
  with ActorLogging
  with Json4sSupport {

  import context.dispatcher
  implicit val serialization = jackson.Serialization
  implicit val system: ActorSystem = context.system

  private val poolClientFlow: Flow[(HttpRequest, Promise[HttpResponse]), (Try[HttpResponse], Promise[HttpResponse]), Http.HostConnectionPool] = {
    Http().cachedHostConnectionPool[Promise[HttpResponse]](
      host = node.host,
      port = node.port
    )
  }

  log.info(s"connecting Ethereum at ${node.host}:${node.port}")

  private val queue: SourceQueueWithComplete[(HttpRequest, Promise[HttpResponse])] =
    Source.queue[(HttpRequest, Promise[HttpResponse])](100, OverflowStrategy.backpressure)
      .via(poolClientFlow)
      .toMat(Sink.foreach({
        case (Success(resp), p) ⇒ p.success(resp)
        case (Failure(e), p)    ⇒ p.failure(e)
      }))(Keep.left).run()(materilizer)

  private def request(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued ⇒
        responsePromise.future
      case QueueOfferResult.Dropped ⇒
        Future.failed(new RuntimeException("Queue overflowed."))
      case QueueOfferResult.Failure(ex) ⇒
        Future.failed(ex)
      case QueueOfferResult.QueueClosed ⇒
        Future.failed(new RuntimeException("Queue closed."))
    }
  }

  private def handle(req: JsonRpcReq): Future[JsonRpcRes] = {

    val entity = HttpEntity(ContentTypes.`application/json`, req.json)
    for {
      httpResp ← request(HttpRequest(method = HttpMethods.POST, entity = entity))
      jsonStr ← httpResp.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
    } yield JsonRpcRes(jsonStr)
  }

  def receive: Receive = {
    case req: JsonRpcReq ⇒ handle(req) pipeTo sender
  }
}
