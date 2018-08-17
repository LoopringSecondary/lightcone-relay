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
import akka.cluster._
import akka.routing._
import akka.cluster.routing._
import org.loopring.lightcone.core.routing.Routers
import com.typesafe.config.Config
import org.loopring.lightcone.data.deployment._

object OrderUpdateCoordinator
  extends base.Deployable[OrderUpdateCoordinatorSettings] {
  val name = "order_update_coordinator"
  val isSingleton = false

  def props = Props(classOf[OrderUpdateCoordinator])

  def getCommon(s: OrderUpdateCoordinatorSettings) =
    base.CommonSettings("", s.roles, s.instances)
}

class OrderUpdateCoordinator() extends Actor {
  def receive: Receive = {
    case settings: OrderUpdateCoordinatorSettings =>
    case _ =>
  }
}