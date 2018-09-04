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
import org.loopring.lightcone.core.persistence.{ LightconePersistenceModuleImpl, PersistenceModule }
import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._
import slick.basic.DatabaseConfig

import scala.concurrent.{ ExecutionContext, Future }

object OrderDBAccessor
  extends base.Deployable[OrderDBAccessorSettings] {
  val name = "order_db_accessor"
  val isSingleton = false

  def props = Props(classOf[OrderDBAccessor])

  def getCommon(s: OrderDBAccessorSettings) =
    base.CommonSettings("", s.roles, s.instances)
}

class OrderDBAccessor() extends Actor {
  implicit val executor = ExecutionContext.global

  //TODO(xiaolu) replace this by guice
  val module = new LightconePersistenceModuleImpl(DatabaseConfig.forConfig(""))

  def receive: Receive = {
    case settings: OrderDBAccessorSettings =>
    case su: SaveUpdatedOrders =>
    case sc: SoftCancelOrders =>
    case s: SaveOrders =>
      sender ! module.orders.saveOrderEntity()
      sender ! module.db.run(module.orders
        .saveOrderEntity(
          org.loopring.lightcone.core.persistence.entities.Order(0L, "", 0L, 0L)))

    case chainRolledBack: ChainRolledBack => rollbackOrders(chainRolledBack.detectedBlockNumber)
    case changeLogs: NotifyRollbackOrders =>

  }

  def writeToDB(orders: Seq[RawOrder]) = {}
  def rollbackOrders(blockNumber: com.google.protobuf.ByteString) = {
  }
}