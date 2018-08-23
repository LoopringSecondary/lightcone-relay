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
import org.loopring.lightcone.core.actors.base.CommonSettings
import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order.{ RawOrder, SaveOrders, SaveUpdatedOrders, SoftCancelOrders }

import scala.concurrent.{ ExecutionContext, Future }

object OrderChangeLogWriter
  extends base.Deployable[CommonSettings] {
  val name = "order_db_accessor"
  val isSingleton = false

  def props = Props(classOf[OrderChangeLogWriter])

  def getCommon(s: CommonSettings) =
    base.CommonSettings("", s.roles, s.instances)
}

class OrderChangeLogWriter() extends Actor {
  implicit val executor = ExecutionContext.global
  def receive: Receive = {
    case settings: OrderDBAccessorSettings =>
    case su: SaveUpdatedOrders =>
    case sc: SoftCancelOrders =>
    case s: SaveOrders =>
    case chainRolledBack: ChainRolledBack => rollbackOrderChange(chainRolledBack.detectedBlockNumber)
  }

  def rollbackOrderChange(blockNumber: com.google.protobuf.ByteString) = {}
}