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

package org.loopring.lightcone.core.managing

import akka.pattern.{ ask, pipe }
import akka.util.Timeout
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
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.data.deployment._

class NodeHttpServer(
  config: Config,
  nodeManager: ActorRef)(implicit val cluster: Cluster)
  extends Directives
  with Json4sSupport {

  implicit val system = cluster.system
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats
  implicit val timeout = Timeout(2 seconds)

  lazy val route =
    pathPrefix("actors") {
      concat(
        pathEnd {
          concat(
            get {
              val f = for {
                f1 <- (system.actorOf(
                  Props(new LocalActorsDetector("/user/router_*"))) ? "detect")
                  .mapTo[LocalActors]
                f2 <- (system.actorOf(
                  Props(new LocalActorsDetector("/user/role_*"))) ? "detect")
                  .mapTo[LocalActors]
                f3 <- (system.actorOf(
                  Props(new LocalActorsDetector("/user/singleton_*"))) ? "detect")
                  .mapTo[LocalActors]
              } yield {
                LocalNodeSummary(cluster.selfRoles.toSeq, Map(
                  "routers" -> f1,
                  "roles" -> f2,
                  "singletons" -> f3))
              }
              complete(f)
            } // ,
          // post {
          //   entity(as[User]) { user =>
          //     val userCreated: Future[ActionPerformed] =
          //       (userRegistryActor ? CreateUser(user)).mapTo[ActionPerformed]
          //     onSuccess(userCreated) { performed =>
          //       log.info("Created user [{}]: {}", user.name, performed.description)
          //       complete((StatusCodes.Created, performed))
          //     }
          //   }
          // }
          )
        })
    }

  Http().bindAndHandle(route, "localhost", config.getInt("node-manager.http.port"))
}