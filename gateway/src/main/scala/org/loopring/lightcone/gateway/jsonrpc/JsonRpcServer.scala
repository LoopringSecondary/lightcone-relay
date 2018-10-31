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

package org.loopring.lightcone.gateway.jsonrpc

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import com.typesafe.config.Config
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.corundumstudio.socketio.Configuration
import com.google.inject.Inject
import org.loopring.lightcone.gateway.socketio.{ ConnectionListener, DisconnectionListener, EventRegistering, SocketIOServer }
import org.slf4j.LoggerFactory

trait JsonRpcServer {
}

class JsonRpcServerImpl @Inject() (
    implicit
    system: ActorSystem,
    mat: ActorMaterializer
) extends JsonRpcServer {

  lazy val logger = LoggerFactory.getLogger(getClass)

  implicit val ex = system.dispatcher

  // implicit val system = cluster.system

  val jsonRpcService = new JsonRpcService()

  lazy val route =
    pathPrefix("rpc") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[String]) { c ⇒
                complete(jsonRpcService.getAPIResult(c))
              }
            }
          )
        }
      )
    }

  lazy val registering = EventRegistering().registering("getBalance", 10, "balance")

  lazy val bind = Http().bindAndHandle(route, "localhost", system.settings.config.getInt("jsonrpc.http.port"))

  bind onComplete {
    case scala.util.Success(value) ⇒ logger.info(s"http server has started @ $value")
    case scala.util.Failure(ex)    ⇒ logger.error("http server failed", ex)
  }

  lazy val socketIOServer = new SocketIOServer(
    jsonRpcService,
    config = system.settings.config,
    eventRegistering = registering
  )

  try socketIOServer.start
  catch {
    case t: Throwable ⇒ logger.error("socketio started failed!", t)
  }
}
