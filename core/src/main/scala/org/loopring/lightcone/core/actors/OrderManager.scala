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
import org.loopring.lightcone.proto.block_chain_event.OrderCancelled
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._

object OrderManager
  extends base.Deployable[OrderManagerSettings] {
  val name = "order_manager"

  def getCommon(s: OrderManagerSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class OrderManager() extends Actor {
  def receive: Receive = {
    case settings: OrderManagerSettings =>
    case OrdersSaved =>
    case OrdersSoftCancelled =>
    case OrderCancelled =>
    case MarkOrdersDeferred =>
    case MarkOrdersBeingMatched =>
    case MarkOrdersSettling =>
    // 只要订单发生状态变化，以下5步是共同逻辑，个别情况会有例外
    // 1. recalculate order status
    // 2. update cache
    // 3. update db
    // 4. notify socket
    // 5. update db change log
    case OrdersUpdated => // notify socket
    case GetOrder =>
    case GetOrders =>
  }
}