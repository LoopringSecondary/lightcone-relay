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

package org.loopring.lightcone.core.database.entities

import org.loopring.lightcone.core.database.base._

case class Order(
  id: Long = 0L,
  protocol: String,
  delegateAddress: String,
  owner: String,
  authAddress: String,
  privateKey: String,
  walletAddress: String,
  orderHash: String,
  tokenS: String,
  tokenB: String,
  amountS: String,
  amountB: String,
  validSince: Long,
  validUntil: Long,
  lrcFee: String,
  buyNoMoreThanAmountB: Boolean,
  marginSplitPercentage: Int,
  v: Int,
  r: String,
  s: String,
  powNonce: Long,
  updatedBlock: Long,
  dealtAmountS: String,
  dealtAmountB: String,
  cancelledAmountS: String,
  cancelledAmountB: String,
  splitAmountS: String,
  splitAmountB: String,
  status: Int,
  minerBlockMark: Long,
  broadcastTime: Long = 0L,
  market: String,
  side: String,
  orderType: String,
  createdAt: Long,
  updatedAt: Long) extends BaseEntity

