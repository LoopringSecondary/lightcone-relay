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
import org.loopring.lightcone.proto._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import com.typesafe.config.Config
import scala.concurrent.duration._
import akka.http.scaladsl.model.StatusCodes
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.core.actors._
import akka.cluster.pubsub._
import akka.cluster.pubsub.DistributedPubSubMediator._
import org.loopring.lightcone.data.deployment._
import akka.cluster.singleton._
import org.loopring.lightcone.core.routing._
import com.google.protobuf.any.Any

class NodeManager()(implicit val cluster: Cluster)
  extends Actor
  with ActorLogging
  with Timers {

  implicit val system = cluster.system
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(1 seconds)

  Routers.setRouters("cluster_manager", ClusterManager.deploy())

  val http = new NodeHttpServer(self)

  val mediator = DistributedPubSub(system).mediator
  mediator ! Subscribe("cluster_manager", self)

  def receive: Receive = {
    case Msg("get_stats") =>
      val f = system.actorOf(Props(classOf[LocalActorsDetector])) ? Msg("detect")

      f.mapTo[LocalStats.ActorGroup].map {
        actors =>
          LocalStats(cluster.selfRoles.toSeq, Seq(actors))
      }.pipeTo(sender)

    case req: UploadDynamicSettings =>
      Routers.clusterManager ! req

    case ProcessDynamicSettings(Some(settings)) =>
      NodeData.dynamicSettings = settings

      Routers.setRouters(
        ExampleActor.name,
        ExampleActor.deploy(settings.exampleActorSettings))

      Routers.setRouters(
        BalanceCacher.name,
        BalanceCacher.deploy(settings.balanceCacherSettings))

      Routers.setRouters(
        BalanceManager.name,
        BalanceManager.deploy(settings.balanceManagerSettings))
  }

}