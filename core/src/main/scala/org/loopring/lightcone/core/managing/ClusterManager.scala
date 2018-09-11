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

import akka.util.Timeout
import scala.concurrent.ExecutionContext
import akka.actor._
import akka.cluster._
import org.loopring.lightcone.core.routing.Routers
import akka.cluster.pubsub._
import akka.cluster.pubsub.DistributedPubSubMediator._
import scala.concurrent.duration._
import com.typesafe.config.Config
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.proto.deployment._
import akka.cluster.singleton._
import akka.cluster.routing._

object ClusterManager {
  val name = "cluster_manager"

  def deploy()(implicit cluster: Cluster) = {
    val actor = cluster.system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(new ClusterManager),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(cluster.system)),
      name = name)

    Map("" -> actor)
  }
}

class ClusterManager extends Actor {

  val mediator = DistributedPubSub(context.system).mediator
  def receive: Receive = {

    case UploadDynamicSettings(c) =>
      println("UploadDynamicSettings: " + c)
      mediator ! Publish("cluster_manager", ProcessDynamicSettings(c))
  }
}