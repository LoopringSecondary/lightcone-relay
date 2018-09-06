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
import org.loopring.lightcone.core.database.entities._
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
  def status = column[Int]("status", O.SqlType("TINYINT(4)"))
  def minerBlockMark = column[Long]("miner_block_mark")
  def broadcastTime = column[Long]("broadcast_time")
  def market = column[String]("market", O.SqlType("VARCHAR(32)"))
  def side = column[String]("side", O.SqlType("VARCHAR(32)"))
  def orderType = column[String]("order_type", O.SqlType("VARCHAR(32)"))

  def * = (id, rawOrderProjection, updatedBlock, dealtAmountS, dealtAmountB, cancelledAmountS, cancelledAmountB, splitAmountS, splitAmountB, status, minerBlockMark, broadcastTime, powNonce, market, updatedAt, createdAt) <> (Order.tupled, Order.unapply)
  def rawOrderProjection = (protocol, delegateAddress, owner, authAddress, privateKey, walletAddress, orderHash, tokenS, tokenB, amountS, amountB, validSince, validUntil, lrcFee, buyNoMoreThanAmountB, marginSplitPercentage, v, r, s, side, orderType) <> ((RawOrder.apply _).tupled, RawOrder.unapply)

  def idx = index("idx_order_hash", orderHash, unique = true)

}
