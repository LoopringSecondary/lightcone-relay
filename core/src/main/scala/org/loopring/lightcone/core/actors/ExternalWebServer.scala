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

package org.loopring.lightcone.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives
import scalapb.json4s.JsonFormat
import scala.concurrent.Future
import org.json4s.{ DefaultFormats, jackson }
import org.loopring.lightcone.proto.data._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport

class ExternalWebServer()(implicit val system: ActorSystem)
  extends Directives
  with Json4sSupport {

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats

  def start() {
    val route =
      path("hello") {
        get {
          complete {
            ActorDeployment("a", 1, false)
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8081)
  }
}