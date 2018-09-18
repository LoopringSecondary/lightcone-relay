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

package org.loopring.lightcone.core.database.tables

import org.loopring.lightcone.core.database.base._
import org.loopring.lightcone.proto.order._
import slick.jdbc.MySQLProfile.api._

class Orders(tag: Tag) extends BaseTable[Order](tag, "ORDERS") {
  def protocol = column[String]("protocol", O.SqlType("VARCHAR(64)"))
  def delegateAddress = column[String]("delegate_address", O.SqlType("VARCHAR(64)"))
  def owner = column[String]("owner", O.SqlType("VARCHAR(64)"))
  def authAddress = column[String]("auth_address", O.SqlType("VARCHAR(64)"))
  def privateKey = column[String]("private_key", O.SqlType("VARCHAR(128)"))
  def walletAddress = column[String]("wallet_address", O.SqlType("VARCHAR(64)"))
  def orderHash = column[String]("order_hash", O.SqlType("VARCHAR(128)"))
  def tokenS = column[String]("token_s", O.SqlType("VARCHAR(64)"))
  def tokenB = column[String]("token_b", O.SqlType("VARCHAR(64)"))
  def amountS = column[String]("amount_s", O.SqlType("VARCHAR(64)"))
  def amountB = column[String]("amount_b", O.SqlType("VARCHAR(64)"))
  def validSince = column[Long]("valid_since")
  def validUntil = column[Long]("valid_until")
  def lrcFee = column[String]("lrc_fee", O.SqlType("VARCHAR(64)"))
  def buyNoMoreThanAmountB = column[Boolean]("buy_no_more_than_amount_b")
  def marginSplitPercentage = column[Int]("margin_split_percentage", O.SqlType("TINYINT(4)"))
  def v = column[Int]("v", O.SqlType("TINYINT(4)"))
  def r = column[String]("r", O.SqlType("VARCHAR(128)"))
  def s = column[String]("s", O.SqlType("VARCHAR(128)"))
  def powNonce = column[Long]("pow_nonce")
  def updatedBlock = column[Long]("updated_block")
  def dealtAmountS = column[String]("dealt_amount_s", O.SqlType("VARCHAR(64)"))
  def dealtAmountB = column[String]("dealt_amount_b", O.SqlType("VARCHAR(64)"))
  def cancelledAmountS = column[String]("cancelled_amount_s", O.SqlType("VARCHAR(64)"))
  def cancelledAmountB = column[String]("cancelled_amount_b", O.SqlType("VARCHAR(64)"))
  def splitAmountS = column[String]("split_amount_s", O.SqlType("VARCHAR(64)"))
  def splitAmountB = column[String]("split_amount_b", O.SqlType("VARCHAR(64)"))
  def status = column[String]("status", O.SqlType("TINYINT(4)"))
  //  def minerBlockMark = column[Long]("miner_block_mark")
  def broadcastTime = column[Int]("broadcast_time")
  def market = column[String]("market", O.SqlType("VARCHAR(32)"))
  def side = column[String]("side", O.SqlType("VARCHAR(32)"))
  def price = column[Double]("price", O.SqlType("DECIMAL(28,16)"))
  def orderType = column[String]("order_type", O.SqlType("VARCHAR(32)"))

  def * = (
    id,
    rawOrderProjection,
    updatedBlock,
    dealtAmountS,
    dealtAmountB,
    splitAmountS,
    splitAmountB,
    cancelledAmountS,
    cancelledAmountB,
    status,
    broadcastTime,
    price,
    powNonce,
    market,
    side,
    orderType,
    createdAt,
    updatedAt
  ) <> (extendTupled, unwrapOption)

  def rawOrderProjection = (
    protocol,
    delegateAddress,
    tokenS,
    tokenB,
    amountS,
    amountB,
    validSince,
    validUntil,

    lrcFee,
    buyNoMoreThanAmountB,
    marginSplitPercentage,
    owner,
    walletAddress,
    authAddress,
    privateKey,
    v,
    s,
    s,
    orderHash
  ) <> (
      (RawOrder.apply _).tupled,
      RawOrder.unapply
    )

  private def extendTupled = (i: Tuple18[Long, RawOrder, Long, String, String, String, String, String, String, String, Int, Double, Long, String, String, String, Long, Long]) ⇒
    Order.apply(
      i._1,
      Some(i._2),
      i._3,
      i._4,
      i._5,
      i._6,
      i._7,
      i._8,
      i._9,
      Some(wrapStatus(i._10)),
      i._11,
      i._12,
      i._13,
      i._14,
      i._15,
      wrapType(i._16),
      i._17,
      i._18
    )

  private def unwrapOption(order: Order) = {
    val unapplyOrder = Order.unapply(order).get
    Some((
      unapplyOrder._1,
      unapplyOrder._2.get,
      unapplyOrder._3,
      unapplyOrder._4,
      unapplyOrder._5,
      unapplyOrder._6,
      unapplyOrder._7,
      unapplyOrder._8,
      unapplyOrder._9,
      unwrapStatus(unapplyOrder._10),
      unapplyOrder._11,
      unapplyOrder._12,
      unapplyOrder._13,
      unapplyOrder._14,
      unapplyOrder._15,
      unapplyOrder._16.name,
      unapplyOrder._17,
      unapplyOrder._18
    ))

  }

  def idx = index("idx_order_hash", orderHash, unique = true)

  def wrapStatus(src: String): OrderStatus =
    OrderLevel1Status.fromName(src) match {
      case Some(value) ⇒ OrderStatus(
        value,
        OrderLevel2Status.ORDER_STATUS_LEVEL2_UNKNOWN,
        OrderLevel3Status.ORDER_STATUS_LEVEL3_UNKNOWN
      )
      case None ⇒ OrderStatus(
        OrderLevel1Status.ORDER_STATUS_LEVEL1_UNKNOWN,
        OrderLevel2Status.ORDER_STATUS_LEVEL2_UNKNOWN,
        OrderLevel3Status.ORDER_STATUS_LEVEL3_UNKNOWN
      )
    }

  def unwrapStatus(status: Option[OrderStatus]): String =
    status match {
      case Some(value) ⇒ value.level1Status.name
      case None        ⇒ OrderLevel1Status.ORDER_STATUS_LEVEL1_UNKNOWN.name
    }

  def wrapType(src: String): OrderType =
    OrderType.fromName(src) match {
      case Some(value) ⇒ value
      case None        ⇒ OrderType.UNKNOWN
    }
}
