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

package org.loopring.lightcone.core.persistence.tables

import org.loopring.lightcone.core.persistence.base._
import org.loopring.lightcone.proto.order.OrderChangeLog
import slick.jdbc.MySQLProfile.api._

class OrderChangeLogs(tag: Tag) extends BaseTable[OrderChangeLog](tag, "ORDER_CHANGE_LOGS") {
  def orderId = column[String]("order_id", O.SqlType("VARCHAR(100)"))
  def userId = column[String]("user_id", O.SqlType("VARCHAR(100)"))
  def accountCategory = column[String]("account_category", O.SqlType("VARCHAR(100)"))
  def status = column[Int]("status", O.SqlType("TINYINT(4)"))
  def price = column[Double]("price")
  def originalInLimit = column[Long]("original_in_limit")
  def originalOutLimit = column[Long]("original_out_limit")
  def executedInAmount = column[Long]("executed_in_amount")
  def executedOutAmount = column[Long]("executed_out_amount")
  def inLimit = column[Long]("in_limit")
  def outLimit = column[Long]("out_limit")
  def marketId = column[String]("market_id", O.SqlType("VARCHAR(100)"))
  def side = column[Int]("side", O.SqlType("TINYINT(4)"))
  def takerOnly = column[Int]("taker_only", O.SqlType("TINYINT(4)"))
  def refundAmount = column[Long]("refund_amount")
  def refundReason = column[Int]("refund_reason", O.SqlType("TINYINT(4)"))
  def * = (id, orderId, userId, accountCategory, status, price, originalInLimit, originalOutLimit,
    executedInAmount, executedOutAmount, inLimit, outLimit, marketId, side, takerOnly, refundAmount,
    refundReason, createdAt, updatedAt) <> (Order.tupled, Order.unapply)

  def idx = index("idx_order_id", orderId, unique = true)
}