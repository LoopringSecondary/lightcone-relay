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

package org.loopring.lightcone.core.persistence.dals

import org.joda.time.DateTime
import org.loopring.lightcone.core.persistence._
import org.loopring.lightcone.core.persistence.base._
import org.loopring.lightcone.core.persistence.tables._
import org.loopring.lightcone.proto.order.Order
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

trait OrdersDal extends BaseDalImpl[Orders, Order] {
  def getOrder(userId: Option[String], orderId: String): Future[Option[Order]]
  def getOrders(userId: String, status: Seq[Int], orderIds: Seq[String]): Future[Seq[Order]]
  def getOrders(userId: String, status: Seq[Int], skip: Int, take: Int): Future[Seq[Order]]
  def getOrdersList(status: Seq[Int], skip: Int, take: Int): Future[Seq[Order]]
  def saveOrderEntity(order: Order): DBIO[Int]
  def updateOrder(order: Order): DBIO[Int]
  def cancelOrder(order: Order): DBIO[Int]
}

class OrdersDalImpl(val module: PersistenceModule) extends OrdersDal {
  val query = ordersQ

  def saveOrderEntity(order: Order): DBIO[Int] = query += order

  def updateOrder(order: Order): DBIO[Int] = {
    query
      .filter(_.orderHash === order.rawOrder.get.hash)
      .map(r => (r.executedInAmount, r.executedOutAmount, r.inLimit, r.outLimit, r.status, r.updatedAt))
      .update(order.executedInAmount, order.executedOutAmount, order.inLimit, order.outLimit, order.status, order.updatedAt)
  }

  def cancelOrder(order: Order): DBIO[Int] = {
    query
      .filter(_.orderHash === order.rawOrder.get.hash)
      .map(r => (r.status, r.updatedAt))
      .update(order.status, )
  }

  def getOrder(orderHash: String) = {
      findByFilter(_.orderHash === orderHash) .map(_.headOption)
  }

  def getOrders(owner: String, status: Seq[Int], orderHashes: Seq[String]): Future[Seq[Order]] = {
    db.run(query
      .filter(_.owner === owner)
      .filter(_.status inSet status)
      .filter(_.orderHash inSet orderHashes)
      .sortBy(_.createdAt.desc)
      .result)
  }

  def getOrders(userId: String, status: Seq[Int], skip: Int, take: Int): Future[Seq[Order]] = {
    db.run(query
      .filter(_.status inSet status)
      .sortBy(_.createdAt.desc)
      .drop(skip)
      .take(take)
      .result)
  }

  def getOrdersList(status: Seq[Int], skip: Int, take: Int): Future[Seq[Order]] = {
    db.run(query
      .filter(_.status inSet status)
      .sortBy(_.createdAt.desc)
      .drop(skip)
      .take(take)
      .result)
  }
}