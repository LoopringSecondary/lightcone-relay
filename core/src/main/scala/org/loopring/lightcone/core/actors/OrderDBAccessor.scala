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
import org.loopring.lightcone.core.database._
import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.core.database.entities.RawOrder
import com.google.protobuf.ByteString
import org.loopring.lightcone.core.utils.EntityMapper._

import scala.util.{ Failure, Success }

object OrderDBAccessor
  extends base.Deployable[OrderDBAccessorSettings] {
  val name = "order_db_accessor"

  def getCommon(s: OrderDBAccessorSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class OrderDBAccessor(db: OrderDatabase)(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends Actor {

  def receive: Receive = {
    case m: OrderDBAccessorSettings =>
    case m: SaveUpdatedOrders =>
    case m: SoftCancelOrders =>
    case m: SaveOrders =>
      sender ! m.orders.map { o =>
        db.orders.saveOrder(o)
      }

    case m: ChainRolledBack => rollbackOrders(m.detectedBlockNumber)
    case m: NotifyRollbackOrders =>

  }

  def writeToDB(orders: Seq[RawOrder]) = {}
  def rollbackOrders(blockNumber: ByteString) = {
  }

}