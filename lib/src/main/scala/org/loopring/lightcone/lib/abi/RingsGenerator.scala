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

// warning: 代码顺序不能调整！！！！！！
case class RingsGenerator(
    lrcAddress: String,
    ringsInfo: Rings
) {

  val ORDER_VERSION = 0
  val SERIALIZATION_VERSION = 0

  var datastream = BitStream()
  var tablestream = BitStream()
  var orderSpendableSMap = HashMap.empty[String, Int]
  var orderSpendableFeeMap = HashMap.empty[String, Int]

  def toSubmitableParam(): String = {
    val numSpendables = setupSpendables()

    datastream.addNumber(0, 32)
    createMiningTable()
    ringsInfo.orders.map(createOrderTable)

    val stream = BitStream()
    stream.addNumber(SERIALIZATION_VERSION, 2)
    stream.addNumber(ringsInfo.orders.length, 2)
    stream.addNumber(ringsInfo.rings.length, 2)
    stream.addNumber(numSpendables, 2)
    stream.addHex(tablestream.getData)

    ringsInfo.rings.map(ring ⇒ {
      stream.addNumber(ring.orderIdx.length, 1)
      ring.orderIdx.map(o ⇒ stream.addNumber(o, 1))
      stream.addNumber(0, 8 - ring.orderIdx.length)
    })

    stream.addNumber(0, 32)
    stream.addHex(datastream.getData)

    stream.getData
  }

  private def setupSpendables(): Int = {
    var numSpendables = 0
    var ownerTokens = HashMap.empty[String, Int]

    ringsInfo.orders.map(rawOrder ⇒ {
      var retOrder = rawOrder
      val order = rawOrder.getEssential
      require(order.hash.nonEmpty)

      val tokenFee = if (order.feeToken.nonEmpty) order.feeToken else lrcAddress

      val tokensKey = (order.owner + "-" + order.tokenS).toLowerCase
      ownerTokens.get(tokensKey) match {
        case Some(x: Int) ⇒
          orderSpendableSMap += order.hash -> x
        case _ ⇒
          ownerTokens += tokensKey -> numSpendables
          orderSpendableSMap += order.hash -> numSpendables
          numSpendables += 1
      }

      ownerTokens.get((order.owner + "-" + tokenFee).toLowerCase) match {
        case Some(x: Int) ⇒
          orderSpendableFeeMap += order.hash -> x
        case _ ⇒
          ownerTokens += tokensKey -> numSpendables
          orderSpendableFeeMap += order.hash -> numSpendables
          numSpendables += 1
      }
    })

    numSpendables
  }

  // 注意:
  // 1. 对于relay来说miner就是transactionOrigin
  private def createMiningTable(): Unit = {
    require(ringsInfo.miner.nonEmpty)

    val transactionOrigin = if (ringsInfo.transactionOrigin.nonEmpty) ringsInfo.transactionOrigin else ringsInfo.miner
    val feeRecipient = if (ringsInfo.feeRecipient.nonEmpty) ringsInfo.feeRecipient else transactionOrigin

    if (!safeEquals(feeRecipient, transactionOrigin)) {
      insertOffset(datastream.addAddress(ringsInfo.feeRecipient, 20, false))
    } else {
      insertDefault()
    }

    if (!safeEquals(ringsInfo.miner, feeRecipient)) {
      insertOffset(datastream.addAddress(ringsInfo.miner, 20, false))
    } else {
      insertDefault()
    }

    if (ringsInfo.sig.nonEmpty && !safeEquals(ringsInfo.miner, transactionOrigin)) {
      insertOffset(datastream.addHex(createBytes(ringsInfo.sig), false))
      addPadding()
    } else {
      insertDefault()
    }
  }

  private def createOrderTable(rawOrder: RawOrder): Unit = {
    val order = rawOrder.getEssential

    addPadding()
    insertOffset(ORDER_VERSION)
    insertOffset(datastream.addAddress(order.owner, 20, false))
    insertOffset(datastream.addAddress(order.tokenS, 20, false))
    insertOffset(datastream.addAddress(order.tokenB, 20, false))
    insertOffset(datastream.addNumber(order.amountS.asBigInt, 32, false))
    insertOffset(datastream.addNumber(order.amountB.asBigInt, 32, false))
    insertOffset(datastream.addNumber(order.validSince, 4, false))

    orderSpendableSMap.get(order.hash) match {
      case Some(x: Int) ⇒ tablestream.addNumber(x.intValue(), 2)
      case _            ⇒ throw new Exception("ringGenerator get " + order.hash + "orderSpendableS failed")
    }
    orderSpendableFeeMap.get(order.hash) match {
      case Some(x: Int) ⇒ tablestream.addNumber(x.intValue(), 2)
      case _            ⇒ throw new Exception("ringGenerator get " + order.hash + "orderSpendableFee failed")
    }

    if (order.dualAuthAddress.nonEmpty) {
      insertOffset(datastream.addAddress(order.dualAuthAddress, 20, false))
    } else {
      insertDefault()
    }

    // order.broker 默认占位
    insertDefault()

    // order.interceptor默认占位
    insertDefault()

    if (order.wallet.nonEmpty) {
      insertOffset(datastream.addAddress(order.wallet, 20, false))
    } else {
      insertDefault()
    }

    if (order.validUntil > 0) {
      insertOffset(datastream.addNumber(order.validUntil, 4, false))
    } else {
      insertDefault()
    }

    if (rawOrder.sig.nonEmpty) {
      insertOffset(datastream.addHex(createBytes(rawOrder.sig), false))
      addPadding()
    } else {
      insertDefault()
    }

    if (rawOrder.dualAuthSig.nonEmpty) {
      insertOffset(datastream.addHex(createBytes(rawOrder.dualAuthSig), false))
      addPadding()
    } else {
      insertDefault()
    }

    tablestream.addNumber(if (order.allOrNone) 1 else 0, 2)

    if (order.feeToken.nonEmpty && !safeEquals(order.feeToken, lrcAddress)) {
      insertOffset(datastream.addAddress(order.feeToken, 20, false))
    } else {
      insertDefault()
    }

    if (order.feeAmount.asBigInt.compare(BigInt(0)) > 0) {
      insertOffset(datastream.addNumber(order.feeAmount.asBigInt, 32, false))
    } else {
      insertDefault()
    }

    tablestream.addNumber(if (order.feePercentage > 0) order.feePercentage else 0, 2)
    tablestream.addNumber(if (rawOrder.waiveFeePercentage > 0) rawOrder.waiveFeePercentage else 0, 2)
    tablestream.addNumber(if (order.tokenSFeePercentage > 0) order.tokenSFeePercentage else 0, 2)
    tablestream.addNumber(if (order.tokenBFeePercentage > 0) order.tokenBFeePercentage else 0, 2)

    if (order.tokenRecipient.nonEmpty && !safeEquals(order.tokenRecipient, order.owner)) {
      insertOffset(datastream.addAddress(order.tokenRecipient, 20, false))
    } else {
      insertDefault()
    }

    tablestream.addNumber(if (order.walletSplitPercentage > 0) order.walletSplitPercentage else 0, 2)
  }

  private def createBytes(data: String): String = {
    val bitstream = BitStream()
    bitstream.addNumber((data.length - 2) / 2, 32)
    bitstream.addRawBytes(data)
    bitstream.getData
  }

  private def insertOffset(offset: Int): Unit = {
    assert(offset % 4 == 0)
    tablestream.addNumber(offset / 4, 2)
  }

  private def insertDefault(): Unit = {
    tablestream.addNumber(0, 2)
  }

  private def addPadding(): Unit = {
    if (datastream.length % 4 != 0) {
      datastream.addNumber(0, 4 - (datastream.length % 4))
    }
  }

}
