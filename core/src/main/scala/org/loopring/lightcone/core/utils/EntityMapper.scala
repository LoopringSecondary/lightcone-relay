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

package org.loopring.lightcone.core.utils

import org.loopring.lightcone.core.database.entities.{ Order, RawOrder }
import org.loopring.lightcone.proto.order.{ Order => protoOrder }

object EntityMapper {

  implicit def rawOrderToEntity(o: protoOrder): Order = {
    Order.tupled
    val now = System.currentTimeMillis() / 1000

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

}
