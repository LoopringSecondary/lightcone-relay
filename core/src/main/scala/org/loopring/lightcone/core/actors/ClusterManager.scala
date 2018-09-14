/*
 * Copyright 2018 lightcore-relay
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

package org.loopring.lightcone.core.actors

import akka.util.Timeout
import scala.concurrent.ExecutionContext
import akka.actor._
import org.loopring.lightcone.core.routing.Routers
import akka.cluster.pubsub._
import akka.cluster.pubsub.DistributedPubSubMediator._
import scala.concurrent.duration._
import com.typesafe.config.Config
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.proto.deployment._

object ClusterManager extends base.NullConfigDeployable {
  val name = "cluster_manager"
  override val isSingleton = true
}

class ClusterManager()(implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor {

  val mediator = DistributedPubSub(context.system).mediator
  def receive: Receive = {

    case UploadDynamicSettings(c) ⇒
      println("UploadDynamicSettings: " + c)
      mediator ! Publish("cluster_manager", ProcessDynamicSettings(c))
  }
}
