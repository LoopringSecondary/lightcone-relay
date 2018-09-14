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
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import scala.concurrent._
import scalapb.json4s.JsonFormat
import org.json4s._
import org.json4s.native.Serialization
import scala.util._

// TODO(fukun): use a connection pool?
trait JsonRpcSupport {
  implicit val system: ActorSystem
  val ethereumClientFlow: HttpFlow
  val queueSize: Int

  implicit lazy val materializer = ActorMaterializer()
  implicit lazy val executionContext = system.dispatcher

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
      params: Seq[Any]
  )

  private val queue =
    Source.queue[(HttpRequest, Promise[HttpResponse])](queueSize, OverflowStrategy.dropNew)
      .via(ethereumClientFlow)
      .toMat(Sink.foreach({
        case ((Success(resp), p)) ⇒ p.success(resp)
        case ((Failure(e), p))    ⇒ p.failure(e)
      }))(Keep.left)
      .run()

  private def queueRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued ⇒
        responsePromise.future
      case QueueOfferResult.Dropped ⇒
        Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) ⇒
        Future.failed(ex)
      case QueueOfferResult.QueueClosed ⇒
        Future.failed(new RuntimeException(
          "Queue was closed (pool shut down) while running the request. Try again later."
        ))
    }
  }

  def httpPost[R <: scalapb.GeneratedMessage with scalapb.Message[R]](
    method: String
  )(
    params: Seq[Any]
  )(
    implicit
    c: scalapb.GeneratedMessageCompanion[R]
  ): Future[R] = {
    val request = JsonRpcRequest(id, JSON_RPC_VERSION, method, params)
    val jsonReq = Serialization.write(request).toString
    val entity = HttpEntity(ContentTypes.`application/json`, jsonReq)

    val httpReq = HttpRequest(method = HttpMethods.POST, uri = "/", entity = entity)

    for {
      httpRes ← queueRequest(httpReq)
      jsonStr ← httpRes.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
      resp = JsonFormat.parser.fromJsonString[R](jsonStr)
    } yield resp
  }

}
