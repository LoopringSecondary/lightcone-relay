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
import org.joda.time.DateTime
import org.loopring.lightcone.core.database._
import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.core.database.entities.Order
import org.loopring.lightcone.core.database.entities.RawOrder

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

  implicit def rawOrderToEntity(o: org.loopring.lightcone.proto.order.Order): Order = {
    val now = DateTime.now().toDate.getTime / 1000

    val rawOrder = RawOrder(
      o.rawOrder.get.protocol,
      o.rawOrder.get.delegateAddress,
      o.rawOrder.get.owner,
      o.rawOrder.get.authAddr,
      o.rawOrder.get.authPrivateKey,
      o.rawOrder.get.walletAddress,
      o.rawOrder.get.hash,
      o.rawOrder.get.tokenS,
      o.rawOrder.get.tokenB,
      o.rawOrder.get.amountS,
      o.rawOrder.get.amountB,
      o.rawOrder.get.validSince,
      o.rawOrder.get.validUntil,
      o.rawOrder.get.lrcFee,
      o.rawOrder.get.buyNoMoreThanAmountB,
      o.rawOrder.get.marginSplitPercentage.toInt,
      o.rawOrder.get.v,
      o.rawOrder.get.r,
      o.rawOrder.get.s,
      o.rawOrder.get.side.toString(),
      o.rawOrder.get.orderType.toString())

    Order(
      0L,
      rawOrder,
      o.updatedBlock,
      o.dealtAmountS,
      o.dealtAmountB,
      o.cancelledAmountS,
      o.cancelledAmountB,
      o.splitAmountS,
      o.splitAmountB,
      o.status.value,
      0L,
      0L,
      o.rawOrder.get.powNonce,
      o.rawOrder.get.market,
      now,
      now)
  }

  def receive: Receive = {
    case settings: OrderDBAccessorSettings =>
    case su: SaveUpdatedOrders =>
    case sc: SoftCancelOrders =>
    case SaveOrders(orders) =>
      sender ! orders.map { o =>
        db.orders.saveOrder(o)
      }

    case chainRolledBack: ChainRolledBack => rollbackOrders(chainRolledBack.detectedBlockNumber)
    case changeLogs: NotifyRollbackOrders =>

  }

  def writeToDB(orders: Seq[RawOrder]) = {}
  def rollbackOrders(blockNumber: com.google.protobuf.ByteString) = {
  }

}