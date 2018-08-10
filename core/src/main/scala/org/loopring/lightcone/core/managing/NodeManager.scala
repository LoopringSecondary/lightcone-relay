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

import akka.pattern._
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
import akka.cluster.pubsub._
import akka.cluster.pubsub.DistributedPubSubMediator._
import org.loopring.lightcone.data.deployment._
import akka.cluster.singleton._
import org.loopring.lightcone.core.routing._

class NodeManager(val config: Config)(implicit val cluster: Cluster)
  extends Actor
  with ActorLogging
  with Timers
  with DeployCapability {

  implicit val system = cluster.system
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(1 seconds)

  val routers = new Routers(config)

  val clusterManager = system.actorOf(
    ClusterSingletonManager.props(
      singletonProps = Props(classOf[ClusterManager], config),
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system)),
    name = "singleton_cluster_manager")

  val mediator = DistributedPubSub(system).mediator
  mediator ! Subscribe("cluster_manager", self)

  val http = new NodeHttpServer(config, routers, self)

  def receive: Receive = {
    case Msg("get_actors") =>
      val f = for {
        f1 <- (system.actorOf(
          Props(new LocalActorsDetector("/user/router_*"))) ? Msg("detect"))
          .mapTo[LocalActors.Actors]
        f2 <- (system.actorOf(
          Props(new LocalActorsDetector("/user/role_*"))) ? Msg("detect"))
          .mapTo[LocalActors.Actors]
        f3 <- (system.actorOf(
          Props(new LocalActorsDetector("/user/singleton_*"))) ? Msg("detect"))
          .mapTo[LocalActors.Actors]
        localActors = LocalActors(cluster.selfRoles.toSeq, Map(
          "routers" -> f1,
          "roles" -> f2,
          "singletons" -> f3))
      } yield localActors
      f pipeTo sender
  }
}