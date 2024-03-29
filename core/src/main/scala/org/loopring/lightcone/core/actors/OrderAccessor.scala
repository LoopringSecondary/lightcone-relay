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

import akka.actor.Actor
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import akka.pattern.ask
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._

object OrderAccessor
  extends base.Deployable[OrderAccessorSettings] {
  val name = "order_accessor"

  def getCommon(s: OrderAccessorSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class OrderAccessor()(implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor {

  def receive: Receive = {
    case settings: OrderAccessorSettings ⇒
    case m: SaveOrders ⇒ sender ! Routers.orderDBAccessor ? m
    case m: SoftCancelOrders ⇒ sender ! Routers.orderDBAccessor ? m

    case any ⇒ Routers.orderDBAccessor forward any
  }
}
