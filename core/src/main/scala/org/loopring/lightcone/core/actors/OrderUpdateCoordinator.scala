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

import akka.util.Timeout

import scala.concurrent.ExecutionContext
import akka.actor._
import akka.cluster._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Publish, Subscribe }
import akka.routing._
import akka.cluster.routing._
import org.loopring.lightcone.core.routing.Routers
import com.typesafe.config.Config
import org.loopring.lightcone.proto.deployment._
import com.google.inject._
import org.loopring.lightcone.proto.order._

object OrderUpdateCoordinator
  extends base.Deployable[OrderUpdateCoordinatorSettings] {
  val name = "order_update_coordinator"

  def getCommon(s: OrderUpdateCoordinatorSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class OrderUpdateCoordinator()(implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor {

  val name = OrderUpdateCoordinator.name
  val mediator = DistributedPubSub(context.system).mediator

  def receive: Receive = {
    case settings: OrderUpdateCoordinatorSettings ⇒
    case m: MarkOrdersDeferred ⇒

    //      val updatedOrders = UpdatedOrders(UpdatedOrder(order = m.deferOrders))
    //      mediator ! Publish(name, updatedOrders)
    case _ ⇒
  }
}
