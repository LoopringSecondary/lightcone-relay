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
  var currentClusterConfig: Option[ClusterConfig] = None

  deployActorByName("cluster_manager")

  val routers = new Routers(config)

  val mediator = DistributedPubSub(system).mediator
  mediator ! Subscribe("cluster_manager", self)

  val http = new NodeHttpServer(config, routers, self)

  //   message ActorDeployment {
  //     string actor_name = 1;
  //     repeated string roles = 2;
  //     int32 num_instances_per_node = 3;
  //     string market_id = 4;
  // }

  def receive: Receive = {
    case Msg("get_stats") =>
      val f = system.actorOf(Props(classOf[LocalActorsDetector])) ? Msg("detect")

      f.mapTo[LocalStats.ActorGroup].map {
        actors =>
          LocalStats(cluster.selfRoles.toSeq, Seq(actors))
      }.pipeTo(sender)

    case newc: ClusterConfig =>
      val oldc = currentClusterConfig.getOrElse(ClusterConfig(version = -1))

      // if (oldc.version < newc.version) {
      if (true) {
        val clusterRoleSet = cluster.selfRoles.toSet;

        val oldDeployMap = oldc.actorDeployments
          .filter(_.roles.toSet.intersect(clusterRoleSet).nonEmpty)
          .map(d => (d.actorName, d)).toMap

        val newDeployMap = newc.actorDeployments
          .filter(_.roles.toSet.intersect(clusterRoleSet).nonEmpty)
          .map(d => (d.actorName, d)).toMap

        // // stop all actors that are in the old map but not in the new map
        oldDeployMap
          .filter(kv => !newDeployMap.contains(kv._1))
          .foreach { kv =>
            system.actorSelection(s"/user/service_${kv._1}_*") ! PoisonPill
          }

        newDeployMap
          .filter(kv => !oldDeployMap.contains(kv._1))
          .foreach { kv =>

            (0 until kv._2.numInstancesPerNode) foreach { i =>
              deployActorByName(kv._1)
            }
          }

        newDeployMap
          .filter(kv => oldDeployMap.contains(kv._1))
          .foreach { kv =>
            val oldDeploy = oldDeployMap(kv._1)
            val newDeploy = kv._2

            if (oldDeploy.numInstancesPerNode > newDeploy.numInstancesPerNode ||
              oldDeploy.marketId != newDeploy.marketId) {
              system.actorSelection(s"/user/service_${kv._1}_*") ! PoisonPill

              (0 until newDeploy.numInstancesPerNode) foreach { i =>
                deployActorByName(kv._1)
              }
            } else {
              val extra = newDeploy.numInstancesPerNode - oldDeploy.numInstancesPerNode
              (0 until extra) foreach { i =>
                deployActorByName(kv._1)
              }
            }
          }

      }

      currentClusterConfig = Some(newc)
  }
}