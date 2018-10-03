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

import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import akka.actor._
import akka.cluster._
import akka.http.scaladsl.Http
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.model._
import akka.stream._
import akka.pattern.AskTimeoutException
import akka.event.LogSource
import scalapb.json4s.JsonFormat
import scala.concurrent._
import scala.concurrent.duration._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import com.typesafe.config.Config
import org.json4s._
import org.loopring.ethcube.proto.data._

object EthereumProxyEndpoints {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName
    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }
}

class EthereumProxyEndpoints(ethereumProxy: ActorRef)(implicit
    system: ActorSystem,
    materializer: ActorMaterializer
)
  extends Json4sSupport {

  implicit val context = system.dispatcher
  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats
  implicit val timeout = Timeout(3 seconds)
  val log = Logging(system, this)

  def getRoutes(): Route = {
    val exceptionHandler = ExceptionHandler {
      case e: Throwable ⇒
        log.error(e.getMessage)
        complete(errorResponse(e.getMessage))
    }
    handleExceptions(exceptionHandler)(route)
  }

  private lazy val route = pathEndOrSingleSlash {
    concat(
      post {
        entity(as[JsonRpcReqWrapped]) { req ⇒
          val f = (ethereumProxy ? req.toPB).mapTo[JsonRpcRes].map(JsonRpcResWrapped.toJsonRpcResWrapped)
          complete(f)
        }
      }
    )
  } ~ pathPrefix("batch") {
    concat(
      pathEnd {
        concat(
          post {
            // reqs/reqs require : [{}, {}]
            entity(as[Seq[JsonRpcReqWrapped]]) { reqs ⇒
              val f = Future.sequence(
                reqs.map(r ⇒ (ethereumProxy ? r.toPB).mapTo[JsonRpcRes]
                  .map(JsonRpcResWrapped.toJsonRpcResWrapped))
              )

              complete(f)
            }
          }
        )
      }
    )
  }

  private def errorResponse(msg: String): HttpResponse = {
    HttpResponse(
      StatusCodes.InternalServerError,
      entity = HttpEntity(
        ContentTypes.`application/json`,
        s"""{"jsonrpc":"2.0", "error": {"code": 500, "message": "${msg}"}}"""
      )
    )
  }
}
