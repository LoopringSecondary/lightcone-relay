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
import akka.actor._
import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.core.order.OrderAccessHelper
import org.loopring.lightcone.lib.etypes._

import scala.concurrent._
import scala.util.{ Failure, Success }

object OrderDBAccessor
  extends base.Deployable[OrderDBAccessorSettings] {
  val name = "order_db_accessor"

  def getCommon(s: OrderDBAccessorSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class OrderDBAccessor(helper: OrderAccessHelper)(implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor {

  def receive: Receive = {
    case m: OrderDBAccessorSettings ⇒
    case m: SaveUpdatedOrders       ⇒
    case m: SoftCancelOrders ⇒
      sender ! helper.softCancelOrders(m.cancelOption).map(FatOrdersSoftCancelled(_))
    case m: SaveOrders           ⇒ sender ! Future.sequence(m.orders.map(helper.saveOrder))
    case m: ChainRolledBack      ⇒ rollbackOrders(m.detectedBlockNumber.asBigInteger.longValue())
    case m: NotifyRollbackOrders ⇒
    case m: GetOrder ⇒ helper.getOrderByHash(m.orderHash) onComplete {
      case Success(v) ⇒ sender ! OneOrder(v)
      case Failure(_) ⇒ sender ! OneOrder(None)
    }
    case m: GetOrders ⇒ helper.pageQueryOrders(m.query, m.page) onComplete {
      case Success(v) ⇒ sender ! v
      case Failure(_) ⇒ sender ! MultiOrders()
    }
  }

  def writeToDB(orders: Seq[RawOrder]) = {}
  def rollbackOrders(blockNumber: Long) = {}
}
