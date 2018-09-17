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

package org.loopring.lightcone.core.database.dals

import org.loopring.lightcone.core.database.OrderDatabase
import org.loopring.lightcone.core.database.base._
import org.loopring.lightcone.core.database.tables._
import org.loopring.lightcone.proto.order.Order
import org.loopring.lightcone.proto.order.{ OrderChangeLog, OrderStatus }
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class QueryCondition(delegateAddress: String, owner: Option[String],
  market: Option[String], status: Seq[String], orderHashes: Seq[String],
  orderType: Option[String], side: Option[String])

trait OrdersDal extends BaseDalImpl[Orders, Order] {
  def getOrder(orderHash: String): Future[Option[Order]]
  def getOrders(condition: QueryCondition, skip: Int, take: Int): Future[Seq[Order]]
  def getOrdersWithCount(condition: QueryCondition, skip: Int, take: Int): (Future[Seq[Order]], Future[Int])
  def saveOrder(order: Order): Future[Int]
  def softCancelByMarket(owner: String, market: String): Future[Int]
  def softCancelByOwner(owner: String): Future[Int]
  def softCancelByTime(cutoff: Long): Future[Int]
  def softCancelByHash(orderHash: String): Future[Int]
}

class OrdersDalImpl(val module: OrderDatabase) extends OrdersDal {
  val query = ordersQ

  override def update(row: Order): Future[Int] = {
    db.run(query.filter(_.id === row.id).update(row))
  }

  override def update(rows: Seq[Order]): Future[Unit] = {
    db.run(DBIO.seq(rows.map(r => query.filter(_.id === r.id).update(r)): _*))
  }

  def saveOrder(order: Order): Future[Int] = module.db.run(query += order)

  def getOrder(orderHash: String): Future[Option[Order]] = {
    findByFilter(_.orderHash === orderHash).map(_.headOption)
  }

  def getOrders(condition: QueryCondition, skip: Int, take: Int): Future[Seq[Order]] = {

    db.run(unwrapContition(condition)
      .drop(skip)
      .take(take)
      .result)
  }

  def getOrdersWithCount(condition: QueryCondition, skip: Int, take: Int): (Future[Seq[Order]], Future[Int]) = {

    val action = unwrapContition(condition)
      .drop(skip)
      .take(take)

    (db.run(action.drop(skip).take(take).result), db.run(action.length.result))
  }

  def softCancelByMarket(owner: String, market: String): Future[Int] = {
    db.run(query
      .filter(_.owner === owner)
      .filter(_.market === market)
      .map(o => (o.status, o.updatedAt))
      .update(OrderStatus.SOFT_CANCELLED.name, System.currentTimeMillis / 1000))
  }

  def softCancelByOwner(owner: String): Future[Int] = {
    db.run(query
      .filter(_.owner === owner)
      .map(o => (o.status, o.updatedAt))
      .update(OrderStatus.SOFT_CANCELLED.name, System.currentTimeMillis / 1000))
  }

  def softCancelByTime(cutoff: Long): Future[Int] = {
    db.run(query
      .filter(_.validUntil >= cutoff)
      .map(o => (o.status, o.updatedAt))
      .update(OrderStatus.SOFT_CANCELLED.name, System.currentTimeMillis / 1000))
  }

  def softCancelByHash(orderHash: String): Future[Int] = {
    db.run(query
      .filter(_.orderHash === orderHash)
      .map(o => (o.status, o.updatedAt))
      .update(OrderStatus.SOFT_CANCELLED.name, System.currentTimeMillis / 1000))
  }

  def unwrapContition(condition: QueryCondition) = {

    query
      .filter { o =>
        condition.owner.map(o.owner === _).getOrElse(true: Rep[Boolean])
      }
      .filter { o =>
        condition.orderType.map(o.orderType === _).getOrElse(true: Rep[Boolean])
      }
      .filter { o =>
        condition.side.map(o.side === _).getOrElse(true: Rep[Boolean])
      }
      .filter { o =>
        condition.market.map(o.market === _).getOrElse(true: Rep[Boolean])
      }
      .filter(_.status inSet condition.status)
      .filter(_.orderHash inSet condition.orderHashes)
      .sortBy(_.createdAt.desc)
  }
}