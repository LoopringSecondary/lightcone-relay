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

package org.loopring.lightcone.gateway.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import org.loopring.lightcone.gateway.jsonrpc.JsonRpcServer
import org.loopring.lightcone.gateway.socketio.SocketIOServer
import org.slf4j.LoggerFactory

class HttpAndIOServer @Inject() (
    jsonRpcServer: JsonRpcServer,
    ioServer: SocketIOServer
)(
    implicit
    system: ActorSystem,
    mat: ActorMaterializer
) {

  lazy val logger = LoggerFactory.getLogger(getClass)

  implicit val ex = system.dispatcher

  lazy val route =
    pathPrefix("rpc") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[String]) { json ⇒
                // TODO 这里还需要测试 返回的可能是 text 需要转成 application/json
                // 可能需要用 entity 包装
                complete(jsonRpcServer.handleRequest(json))
              }
            }
          )
        }
      )
    }

  lazy val bind = Http().bindAndHandle(route, "localhost", system.settings.config.getInt("jsonrpc.http.port"))

  bind onComplete {
    case scala.util.Success(value) ⇒ logger.info(s"http server has started @ $value")
    case scala.util.Failure(ex)    ⇒ logger.error("http server failed", ex)
  }

  try ioServer.start
  catch {
    case t: Throwable ⇒ logger.error("socketio started failed!", t)
  }

}
