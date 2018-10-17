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

import scala.collection.mutable.HashMap
import org.loopring.lightcone.proto.order.RawOrder
import org.loopring.lightcone.proto.ring._
import org.loopring.lightcone.lib.etypes._

case class RingsGenerator(ringsInfo: Rings) {

  val ORDER_VERSION = 0
  val SERIALIZATION_VERSION = 0
  val lrcAddress = ""
  // key is order.owner
  var orderSpendableIdxMap = HashMap.empty[String, OrderSpendableIdx]

  val paramDataStream = Bitstream("")
  val paramTableStream = Bitstream("")

  def toSubmitableParam(rings: Rings): String = {
    val numSpendables = setupSpendables(rings)

    paramDataStream.addNumber(0, 32)
    createMiningTable(ringsInfo)
    ringsInfo.orders.map(createOrderTable)

    val stream = Bitstream("")
    stream.addNumber(SERIALIZATION_VERSION, 2)
    stream.addNumber(rings.orders.length, 2)
    stream.addNumber(rings.rings.length, 2)
    stream.addNumber(numSpendables, 2)
    stream.addHex(paramTableStream.getData)

    rings.rings.map(ring => {
      stream.addNumber(ring.orderIdx.length, 1)
      ring.orderIdx.map(o => stream.addNumber(o, 1))
      stream.addNumber(0, 8 - ring.orderIdx.length)
    })

    stream.addNumber(0, 32)
    stream.addHex(paramDataStream.getData)

    stream.getData
  }

  private def setupSpendables(rings: Rings): Int = {
    var numSpendables = 0
    var ownerTokens = HashMap.empty[String, HashMap[String, Int]]

    rings.orders.map(order ⇒ {
      val tokenFee = if (order.feeToken.nonEmpty) order.feeToken else lrcAddress
      var ownermap = ownerTokens.getOrElse(order.owner, HashMap.empty[String, Int])

      if (!ownermap.contains(order.tokenS)) {
        numSpendables += 1
        ownermap += order.tokenS -> numSpendables
        ownerTokens += order.owner -> ownermap
        var idx = orderSpendableIdxMap.getOrElse(order.hash, OrderSpendableIdx(order.hash, 0, 0))
        orderSpendableIdxMap += order.hash -> idx.copy(tokenSpendableSIdx = numSpendables)
      }

      if (!ownermap.contains(tokenFee)) {
        numSpendables += 1
        ownermap += tokenFee -> numSpendables
        ownerTokens += order.owner -> ownermap
        var idx = orderSpendableIdxMap.getOrElse(order.hash, OrderSpendableIdx(order.hash, 0, 0))
        orderSpendableIdxMap += order.hash -> idx.copy(tokenSpendableFeeIdx = numSpendables)
      }
    })

    numSpendables
  }

  // 注意:
  // 1. 如果ringsinfo没有填写feeReceipt，那么feeReceipt为tx.sender
  private def createMiningTable(ringsInfo: Rings): Unit = {
    val feeRecipient = if (!ringsInfo.feeRecipient.isEmpty) ringsInfo.feeRecipient else ringsInfo.miner
    val miner = ringsInfo.miner
    val transactionOrigin = miner

    if (!safeEquals(feeRecipient, transactionOrigin)) {
      insertOffset(paramDataStream.addAddress(ringsInfo.feeRecipient, 20, false))
    } else {
      insertDefault()
    }

    if (!safeEquals(miner, feeRecipient)) {
      insertOffset(paramDataStream.addAddress(ringsInfo.miner, 20, false))
    } else {
      insertDefault()
    }

    if (ringsInfo.sig.nonEmpty && !safeEquals(miner, transactionOrigin)) {
      insertOffset(paramDataStream.addHex(createBytes(ringsInfo.sig), false))
      addPadding()
    } else {
      insertDefault()
    }
  }

  private createOrderTable(order: OrderInfo, param: RingsSubmitParam) {

    if (order.orderInterceptor) {
      this.insertOffset(param, param.data.addAddress(order.orderInterceptor, 20, false));
    } else {
      this.insertDefault(param);
    }

    if (order.walletAddr) {
      this.insertOffset(param, param.data.addAddress(order.walletAddr, 20, false));
    } else {
      this.insertDefault(param);
    }

    if (order.validUntil) {
      this.insertOffset(param, param.data.addNumber(order.validUntil, 4, false));
    } else {
      this.insertDefault(param);
    }

    if (order.sig) {
      this.insertOffset(param, param.data.addHex(this.createBytes(order.sig), false));
      this.addPadding(param);
    } else {
      this.insertDefault(param);
    }

    if (order.dualAuthSig) {
      this.insertOffset(param, param.data.addHex(this.createBytes(order.dualAuthSig), false));
      this.addPadding(param);
    } else {
      this.insertDefault(param);
    }

    param.tables.addNumber(order.allOrNone ? 1 : 0, 2);

    if (order.feeToken && order.feeToken !== this.context.lrcAddress) {
      this.insertOffset(param, param.data.addAddress(order.feeToken, 20, false));
    } else {
      this.insertDefault(param);
    }

    if (order.feeAmount) {
      this.insertOffset(param, param.data.addNumber(order.feeAmount, 32, false));
    } else {
      this.insertDefault(param);
    }

    param.tables.addNumber(order.feePercentage ? order.feePercentage : 0, 2);
    param.tables.addNumber(order.waiveFeePercentage ? order.waiveFeePercentage : 0, 2);
    param.tables.addNumber(order.tokenSFeePercentage ? order.tokenSFeePercentage : 0, 2);
    param.tables.addNumber(order.tokenBFeePercentage ? order.tokenBFeePercentage : 0, 2);

    if (order.tokenRecipient && order.tokenRecipient !== order.owner) {
      this.insertOffset(param, param.data.addAddress(order.tokenRecipient, 20, false));
    } else {
      this.insertDefault(param);
    }

    param.tables.addNumber(order.walletSplitPercentage ? order.walletSplitPercentage : 0, 2);
  }

  private def createOrderTable(order: RawOrder): Unit = {
    addPadding()
    insertOffset(ORDER_VERSION)
    insertOffset(paramDataStream.addAddress(order.owner, 20, false))
    insertOffset(paramDataStream.addAddress(order.tokenS, 20, false))
    insertOffset(paramDataStream.addAddress(order.tokenB, 20, false))
    insertOffset(paramDataStream.addNumber(order.amountS.asBigInt, 32, false))
    insertOffset(paramDataStream.addNumber(order.amountB.asBigInt, 32, false))
    insertOffset(paramDataStream.addNumber(order.validSince, 4, false))

    orderSpendableIdxMap.get(order.hash) match {
      case Some(x: OrderSpendableIdx) ⇒ paramTableStream.addNumber(x.tokenSpendableSIdx, 2)
      case _ ⇒ throw new Exception("can't find order " + order.hash + "tokenSpendableS.index")
    }
    orderSpendableIdxMap.get(order.hash) match {
      case Some(x: OrderSpendableIdx) ⇒ paramTableStream.addNumber(x.tokenSpendableFeeIdx, 2)
      case _ ⇒ throw new Exception("can't find order " + order.hash + "tokenSpendableFee.index")
    }

    if (order.dualAuthAddress.nonEmpty) {
      insertOffset(paramDataStream.addAddress(order.dualAuthAddress, 20, false))
    } else {
      insertDefault()
    }

    // order.broker 默认占位
    insertDefault()

    // order.interceptor默认占位
    insertDefault()

    if (order.wallet.nonEmpty) {
      insertOffset(paramDataStream.addAddress(order.wallet, 20, false))
    } else {
      insertDefault()
    }

    if (order.validUntil > 0) {
      insertOffset(paramDataStream.addNumber(order.validUntil, 4, false))
    } else {
      insertDefault()
    }

    if (order.sig.nonEmpty) {
      insertOffset(paramDataStream.addHex(createBytes(order.sig), false))
      addPadding()
    } else {
      insertDefault()
    }

    if (order.dualAuthSig.nonEmpty) {
      insertOffset(paramDataStream.addHex(createBytes(order.dualAuthSig), false))
      addPadding()
    } else {
      insertDefault()
    }

    paramTableStream.addNumber(if (order.allOrNone) 1 else 0, 2);

    if (order.feeToken.nonEmpty && !safeEquals(order.feeToken, lrcAddress)) {
      insertOffset(paramDataStream.addAddress(order.feeToken, 20, false))
    } else {
      insertDefault()
    }

    if (order.feeAmount.nonEmpty) {
      insertOffset(paramDataStream.addNumber(order.feeAmount.asBigInt, 32, false))
    } else {
      insertDefault()
    }

    paramTableStream.addNumber(if (order.feePercentage > 0) order.feePercentage else 0, 2)
    paramTableStream.addNumber(if (order.waiveFeePercentage > 0) order.waiveFeePercentage else 0, 2)
    paramTableStream.addNumber(if (order.tokenSFeePercentage > 0) order.tokenSFeePercentage else 0, 2)
    paramTableStream.addNumber(if (order.tokenBFeePercentage > 0) order.tokenBFeePercentage else 0, 2)

    if (order.tokenRecipient.nonEmpty && !safeEquals(order.tokenRecipient, order.owner)) {
      insertOffset(paramDataStream.addAddress(order.tokenRecipient, 20, false))
    } else {
      insertDefault()
    }

    paramTableStream.addNumber(if (order.walletSplitPercentage > 0) order.walletSplitPercentage else 0, 2)
  }

  private def createBytes(data: String): String = {
    val bitstream = Bitstream("")
    bitstream.addNumber((data.length - 2) / 2, 32)
    bitstream.addRawBytes(data)
    bitstream.getData
  }

  private def insertOffset(offset: Int): Unit = {
    assert(offset % 4 == 0)
    paramTableStream.addNumber(offset / 4, 2)
  }

  private def insertDefault(): Unit = {
    paramTableStream.addNumber(0, 2)
  }

  private def addPadding(): Unit = {
    if (paramDataStream.length % 4 != 0) {
      paramDataStream.addNumber(0, 4 - (paramDataStream.length % 4))
    }
  }

}
