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

package org.loopring.lightcone.core.actors

import akka.actor._
import org.loopring.lightcone.core.routing.Routers
import akka.cluster.pubsub._
import akka.cluster.pubsub.DistributedPubSubMediator._
import scala.concurrent.duration._
import com.typesafe.config.Config
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.data.deployment._

class ClusterManager() extends Actor {

  val mediator = DistributedPubSub(context.system).mediator
  var currentConfig = DynamicSettings()

  def receive: Receive = {
    case Msg("get_config") =>
      sender ! currentConfig

    case UploadDynamicSettings(c) =>
      mediator ! Publish("cluster_manager", ProcessDynamicSettings(c))
  }
}