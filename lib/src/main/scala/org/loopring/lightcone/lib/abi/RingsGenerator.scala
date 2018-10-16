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

package org.loopring.lightcone.lib.abi

import org.loopring.lightcone.proto.order.RawOrder
import org.loopring.lightcone.proto.ring._
import org.loopring.lightcone.lib.etypes._

case class RingsGenerator(lrcAddress: String) {

  val ORDER_VERSION = 0

  // 注意:
  // 1. 如果ringsinfo没有填写feeReceipt，那么feeReceipt为tx.sender
  private def createMiningTable(ringsInfo: Rings, param: RingsSubmitParam): Unit = {
    val feeRecipient = if (!ringsInfo.feeRecipient.isEmpty) ringsInfo.feeRecipient else ringsInfo.miner
    val miner = ringsInfo.miner
    val transactionOrigin = miner

    if (!safeEquals(feeRecipient, transactionOrigin)) {
      this.insertOffset(param, param.data.addAddress(ringsInfo.feeRecipient, 20, false))
    } else {
      this.insertDefault(param)
    }

    if (!safeEquals(miner, feeRecipient)) {
      this.insertOffset(param, param.data.addAddress(ringsInfo.miner, 20, false))
    } else {
      this.insertDefault(param)
    }

    if (ringsInfo.sig.nonEmpty && !safeEquals(miner, transactionOrigin)) {
      this.insertOffset(param, param.data.addHex(this.createBytes(ringsInfo.sig), false))
      this.addPadding(param)
    } else {
      this.insertDefault(param)
    }
  }

  private def createOrderTable(order: RawOrder, param: RingsSubmitParam) {
    this.addPadding(param)
    this.insertOffset(param, this.ORDER_VERSION)
    this.insertOffset(param, param.data.addAddress(order.owner, 20, false))
    this.insertOffset(param, param.data.addAddress(order.tokenS, 20, false))
    this.insertOffset(param, param.data.addAddress(order.tokenB, 20, false))
    this.insertOffset(param, param.data.addNumber(order.amountS.asBigInt, 32, false))
    this.insertOffset(param, param.data.addNumber(order.amountB.asBigInt, 32, false))
    this.insertOffset(param, param.data.addNumber(order.validSince, 4, false))

    // todo: 是否有参与到ring.data的生成
//    param.tables.addNumber(order.tokenSpendableS.index, 2)
//    param.tables.addNumber(order.tokenSpendableFee.index, 2)

    if (order.dualAuthAddress.nonEmpty) {
      this.insertOffset(param, param.data.addAddress(order.dualAuthAddress, 20, false))
    } else {
      this.insertDefault(param)
    }

    // order.broker 默认占位
    this.insertDefault(param)

    // order.interceptor默认占位
    this.insertDefault(param)

    if (order.wallet.nonEmpty) {
      this.insertOffset(param, param.data.addAddress(order.wallet, 20, false))
    } else {
      this.insertDefault(param)
    }

    if (order.validUntil > 0) {
      this.insertOffset(param, param.data.addNumber(order.validUntil, 4, false))
    } else {
      this.insertDefault(param)
    }

    if (order.sig.nonEmpty) {
      this.insertOffset(param, param.data.addHex(this.createBytes(order.sig), false))
      this.addPadding(param)
    } else {
      this.insertDefault(param)
    }

    if (order.dualAuthSig.nonEmpty) {
      this.insertOffset(param, param.data.addHex(this.createBytes(order.dualAuthSig), false))
      this.addPadding(param)
    } else {
      this.insertDefault(param)
    }

    param.tables.addNumber(if(order.allOrNone) 1 else 0, 2);

    if (order.feeToken.nonEmpty && !safeEquals(order.feeToken, this.lrcAddress)) {
      this.insertOffset(param, param.data.addAddress(order.feeToken, 20, false))
    } else {
      this.insertDefault(param)
    }

    if (order.feeAmount.nonEmpty) {
      this.insertOffset(param, param.data.addNumber(order.feeAmount.asBigInt, 32, false))
    } else {
      this.insertDefault(param)
    }

    param.tables.addNumber(if (order.feePercentage > 0) order.feePercentage else 0, 2)
    param.tables.addNumber(if (order.waiveFeePercentage > 0) order.waiveFeePercentage else 0, 2)
    param.tables.addNumber(if (order.tokenSFeePercentage > 0) order.tokenSFeePercentage else 0, 2)
    param.tables.addNumber(if (order.tokenBFeePercentage > 0) order.tokenBFeePercentage else 0, 2)

    if (order.tokenRecipient.nonEmpty && !safeEquals(order.tokenRecipient, order.owner)) {
      this.insertOffset(param, param.data.addAddress(order.tokenRecipient, 20, false))
    } else {
      this.insertDefault(param)
    }

    param.tables.addNumber(if(order.walletSplitPercentage > 0) order.walletSplitPercentage else 0, 2);
  }

  private def createBytes(data: String): String = {
    val bitstream = Bitstream("")
    bitstream.addNumber((data.length - 2) / 2, 32)
    bitstream.addRawBytes(data)
    bitstream.getData
  }

  private def insertOffset(param: RingsSubmitParam, offset: Int): Unit = {
    assert(offset % 4 == 0)
    param.tables.addNumber(offset / 4, 2)
  }

  private def insertDefault(param: RingsSubmitParam): Unit = {
    param.tables.addNumber(0, 2)
  }

  private def addPadding(param: RingsSubmitParam): Unit = {
    if (param.data.length % 4 != 0) {
      param.data.addNumber(0, 4 - (param.data.length % 4))
    }
  }

  private def xor(s1: String, s2: String, numBytes: Int): String = {
//    val x1 = new BN(s1.slice(2), 16);
//    val x2 = new BN(s2.slice(2), 16);
//    val result = x1.xor(x2);
//    return "0x" + result.toString(16, numBytes * 2);
    ""
  }
}
