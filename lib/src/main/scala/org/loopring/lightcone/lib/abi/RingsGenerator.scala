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
    this.createMiningTable(ringsInfo)
    ringsInfo.orders.map(createOrderTable)

    val stream = Bitstream("")
    stream.addNumber(this.SERIALIZATION_VERSION, 2)
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
      this.insertOffset(paramDataStream.addAddress(ringsInfo.feeRecipient, 20, false))
    } else {
      this.insertDefault()
    }

    if (!safeEquals(miner, feeRecipient)) {
      this.insertOffset(paramDataStream.addAddress(ringsInfo.miner, 20, false))
    } else {
      this.insertDefault()
    }

    if (ringsInfo.sig.nonEmpty && !safeEquals(miner, transactionOrigin)) {
      this.insertOffset(paramDataStream.addHex(this.createBytes(ringsInfo.sig), false))
      this.addPadding()
    } else {
      this.insertDefault()
    }
  }

  private def createOrderTable(order: RawOrder) {
    this.addPadding()
    this.insertOffset(this.ORDER_VERSION)
    this.insertOffset(paramDataStream.addAddress(order.owner, 20, false))
    this.insertOffset(paramDataStream.addAddress(order.tokenS, 20, false))
    this.insertOffset(paramDataStream.addAddress(order.tokenB, 20, false))
    this.insertOffset(paramDataStream.addNumber(order.amountS.asBigInt, 32, false))
    this.insertOffset(paramDataStream.addNumber(order.amountB.asBigInt, 32, false))
    this.insertOffset(paramDataStream.addNumber(order.validSince, 4, false))

    // todo: 是否有参与到ring.data的生成
    orderSpendableIdxMap.get(order.hash) match {
      case Some(x: OrderSpendableIdx) ⇒ paramTableStream.addNumber(x.tokenSpendableSIdx, 2)
      case _ ⇒ throw new Exception("can't find order " + order.hash + "tokenSpendableS.index")
    }
    orderSpendableIdxMap.get(order.hash) match {
      case Some(x: OrderSpendableIdx) ⇒ paramTableStream.addNumber(x.tokenSpendableFeeIdx, 2)
      case _ ⇒ throw new Exception("can't find order " + order.hash + "tokenSpendableFee.index")
    }

    if (order.dualAuthAddress.nonEmpty) {
      this.insertOffset(paramDataStream.addAddress(order.dualAuthAddress, 20, false))
    } else {
      this.insertDefault()
    }

    // order.broker 默认占位
    this.insertDefault()

    // order.interceptor默认占位
    this.insertDefault()

    if (order.wallet.nonEmpty) {
      this.insertOffset(paramDataStream.addAddress(order.wallet, 20, false))
    } else {
      this.insertDefault()
    }

    if (order.validUntil > 0) {
      this.insertOffset(paramDataStream.addNumber(order.validUntil, 4, false))
    } else {
      this.insertDefault()
    }

    if (order.sig.nonEmpty) {
      this.insertOffset(paramDataStream.addHex(this.createBytes(order.sig), false))
      this.addPadding()
    } else {
      this.insertDefault()
    }

    if (order.dualAuthSig.nonEmpty) {
      this.insertOffset(paramDataStream.addHex(this.createBytes(order.dualAuthSig), false))
      this.addPadding()
    } else {
      this.insertDefault()
    }

    paramTableStream.addNumber(if (order.allOrNone) 1 else 0, 2);

    if (order.feeToken.nonEmpty && !safeEquals(order.feeToken, this.lrcAddress)) {
      this.insertOffset(paramDataStream.addAddress(order.feeToken, 20, false))
    } else {
      this.insertDefault()
    }

    if (order.feeAmount.nonEmpty) {
      this.insertOffset(paramDataStream.addNumber(order.feeAmount.asBigInt, 32, false))
    } else {
      this.insertDefault()
    }

    paramTableStream.addNumber(if (order.feePercentage > 0) order.feePercentage else 0, 2)
    paramTableStream.addNumber(if (order.waiveFeePercentage > 0) order.waiveFeePercentage else 0, 2)
    paramTableStream.addNumber(if (order.tokenSFeePercentage > 0) order.tokenSFeePercentage else 0, 2)
    paramTableStream.addNumber(if (order.tokenBFeePercentage > 0) order.tokenBFeePercentage else 0, 2)

    if (order.tokenRecipient.nonEmpty && !safeEquals(order.tokenRecipient, order.owner)) {
      this.insertOffset(paramDataStream.addAddress(order.tokenRecipient, 20, false))
    } else {
      this.insertDefault()
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
