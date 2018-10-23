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

package org.loopring.lightcone.core.gateway.jsonrpc

import akka.cluster.Cluster
import akka.http.scaladsl.Http
import com.typesafe.config.Config
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.google.inject.Inject

trait JsonRpcServer {
}

class JsonRpcServerImpl @Inject() (config: Config)(implicit val cluster: Cluster, implicit val materializer: ActorMaterializer) extends JsonRpcServer {

  implicit val system = cluster.system
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

  Http().bindAndHandle(route, "localhost",
    config.getInt("jsonrpc.http.port"))
}
