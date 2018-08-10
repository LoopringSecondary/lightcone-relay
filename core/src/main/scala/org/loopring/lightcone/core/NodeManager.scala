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

import akka.actor._
import akka.cluster._
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
import com.typesafe.config.Config
import scala.concurrent.duration._
import akka.http.scaladsl.model.StatusCodes

class NodeManager(val config: Config)(implicit val cluster: Cluster)
  extends Actor
  with ActorLogging
  with Timers
  with DeployCapability
  with Directives
  with Json4sSupport {

  implicit val system = cluster.system
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats

  val r = new LocalRouters()
  var deployed: List[String] = List.empty[String]
  timers.startSingleTimer("deploy-default", DeployLocalActors(), 5.seconds)

  val route =
    path("actors") {
      get {
        complete {
          DeployedLocalActors(deployed.reverse, cluster.selfRoles.toSeq)
        }
      }
    }
  // ~
  // path("deploy") {
  //   pathEndOrSingleSlash {
  //     post {
  //       entity(as[DeployLocalActors]) { req =>
  //         complete(StatusCodes.Created ->
  //           DeployedLocalActors(deployed.toSeq))
  //       }
  //     }
  //   }
  // }

  Http().bindAndHandle(route, "localhost", 8080)

  def receive: Receive = {
    case DeployLocalActors(deployments) =>
      if (deployments.isEmpty) deployAllBasedOnRoles()
      else deployments.foreach(deploy)

      sender ! DeployedLocalActors(deployed.reverse, cluster.selfRoles.toSeq)
  }
}