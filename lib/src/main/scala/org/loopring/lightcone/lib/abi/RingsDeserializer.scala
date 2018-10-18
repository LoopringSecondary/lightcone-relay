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

import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.proto.ring._
import org.web3j.utils.Numeric

case class RingsDeserializer(lrcAddress: String, encoded: String) {
  val dataparser: BitParser = BitParser(encoded)
  var dataOffset: Int = 0
  var tableOffset: Int = 0
  var spendableList = Seq.empty[String]

  def deserialize(): Rings = {
    val version = this.dataparser.extractUint16(0)
    val numOrders = this.dataparser.extractUint16(2)
    val numRings = this.dataparser.extractUint16(4)
    val numSpendables = this.dataparser.extractUint16(6)

    assert(version.equals(0), "Unsupported serialization format")
    assert(numSpendables > 0, "Invalid number of spendables")

    val miningDataPtr = 8
    val orderDataPtr = miningDataPtr + 3 * 2
    val ringDataPtr = orderDataPtr + (25 * numOrders) * 2
    val dataBlobPtr = ringDataPtr + (numRings * 9) + 32

    (1 to numSpendables).map(_ ⇒ {
      spendableList +:= "0x0"
    })

    dataOffset = dataBlobPtr
    tableOffset = miningDataPtr

    val feeRecipient = nextAddress
    val miner = nextAddress
    val sig = nextBytes
    val orders = setupOrders(orderDataPtr, numOrders)
    val rings = assembleRings(numRings, ringDataPtr, orders)

    Rings(
      miner = miner,
      sig = sig,
      feeRecipient = feeRecipient,
      orders = orders,
      rings = rings
    )
  }

  private def setupOrders(tablesPtr: Int, numOrders: Int): Seq[RawOrder] = {
    tableOffset = tablesPtr
    (1 to numOrders).map(_ ⇒ assembleOrder())
  }

  private def assembleRings(numRings: Int, originOffset: Int, orders: Seq[RawOrder]): Seq[Ring] = {
    var offset = originOffset

    (1 to numRings).map(_ ⇒ {
      val ringsize = dataparser.extractUint8(offset)
      val ring = assembleRing(ringsize, offset + 1, orders)
      offset += 1 + 8
      ring
    })
  }

  private def assembleRing(ringsize: Int, originOffset: Int, orders: Seq[RawOrder]): Ring = {
    var offset = originOffset
    val seq = (1 to ringsize).map(_ ⇒ {
      val orderidx = dataparser.extractUint8(offset)
      offset += 1
      orderidx
    })

    Ring(seq)
  }

  private def assembleOrder(): RawOrder = {
    val version = nextUint16.toString
    val owner = nextAddress
    val tokenS = nextAddress
    val tokenB = nextAddress
    val amountS = Numeric.toHexString(nextUint.toByteArray)
    val amountB = Numeric.toHexString(nextUint.toByteArray)
    val validSince = nextUint32
    val tokenSpendableS = spendableList.apply(nextUint16)
    val tokenSpendableFee = spendableList.apply(nextUint16)
    val dualAuthAddr = nextAddress
    val broker = nextAddress
    val orderInterceptor = nextAddress
    val walletAddr = nextAddress
    val validUntil = nextUint32
    val sig = nextBytes
    val dualAuthSig = nextBytes
    val allOrNone = nextUint16 > 0
    val feeToken = nextAddress
    val feeAmount = Numeric.toHexString(nextUint.toByteArray)
    val feePercentage = nextUint16
    val waiveFeePercentage = toInt16(nextUint16)
    val tokenSFeePercentage = nextUint16
    val tokenBFeePercentage = nextUint16
    val tokenRecipient = nextAddress
    val walletSplitPercentage = nextUint16

    val essentail = RawOrderEssential()
      .withOwner(owner)
      .withTokenS(tokenS)
      .withTokenB(tokenB)
      .withAmountS(amountS)
      .withAmountB(amountB)
      .withValidSince(validSince)
      .withValidUntil(validUntil)
      .withDualAuthAddress(dualAuthAddr)
      .withBroker(broker)
      .withOrderInterceptor(orderInterceptor)
      .withWallet(walletAddr)
      .withAllOrNone(allOrNone)
      .withFeeToken(if (feeToken.nonEmpty) feeToken else lrcAddress)
      .withFeeAmount(feeAmount)
      .withFeePercentage(feePercentage)
      .withTokenSFeePercentage(tokenSFeePercentage)
      .withTokenBFeePercentage(tokenBFeePercentage)
      .withTokenRecipient(if (tokenRecipient.nonEmpty) tokenRecipient else owner)
      .withWalletSplitPercentage(walletSplitPercentage)

    RawOrder()
      .withEssential(essentail)
      .withVersion(version)
      .withTokenSpendableS(tokenSpendableS)
      .withTokenSpendableFee(tokenSpendableFee)
      .withSig(sig)
      .withDualAuthSig(dualAuthSig)
      .withWaiveFeePercentage(waiveFeePercentage)
  }

  private def nextAddress: String = {
    val offset = tupple4GetNextOffset
    if (offset != 0) {
      dataparser.extractAddress(dataOffset + offset)
    } else {
      "0x0"
    }
  }

  private def nextUint: BigInt = {
    val offset = tupple4GetNextOffset
    if (offset != 0) {
      dataparser.extractUint(dataOffset + offset)
    } else {
      BigInt(0)
    }
  }

  private def nextUint16: Int = {
    getNextOffset
  }

  private def nextUint32: Int = {
    val offset = tupple4GetNextOffset
    if (offset != 0) {
      dataparser.extractUint32(dataOffset + offset)
    } else {
      0
    }
  }

  private def nextBytes: String = {
    val offset = tupple4GetNextOffset
    if (offset != 0) {
      val len = dataparser.extractUint(dataOffset + offset).intValue()
      Numeric.toHexString(dataparser.extractBytesX(dataOffset + offset + 32, len))
    } else {
      ""
    }
  }

  private def toInt16(x: BigInt): Int = {
    x.intValue()
  }

  private def tupple4GetNextOffset: Int = {
    getNextOffset * 4
  }

  private def getNextOffset: Int = {
    val offset = dataparser.extractUint16(tableOffset)
    tableOffset += 2
    offset
  }
}
