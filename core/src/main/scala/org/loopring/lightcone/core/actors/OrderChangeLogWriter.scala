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
import org.loopring.lightcone.core.actors.base.DeploymentMetadata
import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order.{ RawOrder, SaveOrders, SaveUpdatedOrders, SoftCancelOrders }
import scala.concurrent._

object OrderChangeLogWriter
  extends base.Deployable[OrderChangeLogWriterSettings] {
  val name = "order_change_log_writer"

  def getMetadata(s: OrderChangeLogWriterSettings) =
    base.DeploymentMetadata(s.roles, s.instances)
}

class OrderChangeLogWriter(
  dynamicSettings: DynamicSettings,
  settings: OrderChangeLogWriterSettings)(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends Actor {

  def receive: Receive = {
    case su: SaveUpdatedOrders =>
    case sc: SoftCancelOrders =>
    case s: SaveOrders =>
    case chainRolledBack: ChainRolledBack => rollbackOrderChange(chainRolledBack.detectedBlockNumber)
  }

  def rollbackOrderChange(blockNumber: com.google.protobuf.ByteString) = {}
}