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

import org.loopring.lightcone.proto.order.{ RawOrder, RawOrderEssential }
import org.web3j.utils.Numeric
import scala.collection.mutable

case class RingsDeserializer() {

  //  private context: Context;
  //
  //  private data: Bitstream;
  //  private spendableList?: Spendable[];
  //
  //  private dataOffset: number = 0;
  //  private tableOffset: number = 0;
  //
  //  constructor(context: Context) {
  //    this.context = context;
  //  }
  //
  //  public deserialize(data: string): [Mining, OrderInfo[], number[][]] {
  //
  //    this.data = new Bitstream(data);
  //
  //    // Header
  //    const version = this.data.extractUint16(0);
  //    const numOrders = this.data.extractUint16(2);
  //    const numRings = this.data.extractUint16(4);
  //    const numSpendables = this.data.extractUint16(6);
  //
  //    // Validation
  //    assert.equal(version, 0, "Unsupported serialization format");
  //    assert(numSpendables > 0, "Invalid number of spendables");
  //
  //    // Calculate data pointers
  //    const miningDataPtr = 8;
  //    const orderDataPtr = miningDataPtr + 3 * 2;
  //    const ringDataPtr = orderDataPtr + (25 * numOrders) * 2;
  //    const dataBlobPtr = ringDataPtr + (numRings * 9) + 32;
  //
  //    this.spendableList = [];
  //    for (let i = 0; i < numSpendables; i++) {
  //      const spendable = {
  //        initialized: false,
  //        amount: new BigNumber(0),
  //        reserved: new BigNumber(0),
  //      };
  //      this.spendableList.push(spendable);
  //    }
  //
  //    this.dataOffset = dataBlobPtr;
  //
  //    // Setup the rings
  //    const mining = this.setupMiningData(miningDataPtr);
  //    const orders = this.setupOrders(orderDataPtr, numOrders);
  //    const rings = this.assembleRings(numRings, ringDataPtr, orders);
  //
  //    // Testing
  //    this.validateSpendables(orders);
  //
  //    return [mining, orders, rings];
  //  }
  //
  //  private setupMiningData(tablesPtr: number) {
  //    this.tableOffset = tablesPtr;
  //    const mining = new Mining(
  //      this.context,
  //      this.nextAddress(),
  //      this.nextAddress(),
  //      this.nextBytes(),
  //    );
  //    return mining;
  //  }
  //
  //  private setupOrders(tablesPtr: number, numOrders: number) {
  //    this.tableOffset = tablesPtr;
  //    const orders: OrderInfo[] = [];
  //    for (let i = 0; i < numOrders; i++) {
  //      orders.push(this.assembleOrder());
  //    }
  //    return orders;
  //  }
  //
  //
  //  private assembleRings(numRings: number, offset: number, orders: OrderInfo[]) {
  //    const rings: number[][] = [];
  //    for (let i = 0; i < numRings; i++) {
  //      const ringSize = this.data.extractUint8(offset);
  //      const ring = this.assembleRing(ringSize, offset + 1, orders);
  //      rings.push(ring);
  //      offset += 1 + 8;
  //    }
  //    return rings;
  //  }
  //
  //  private assembleRing(ringSize: number, offset: number, orders: OrderInfo[]) {
  //    const ring: number[] = [];
  //    for (let i = 0; i < ringSize; i++) {
  //      const orderIndex = this.data.extractUint8(offset);
  //      offset += 1;
  //      ring.push(orderIndex);
  //    }
  //
  //    return ring;
  //  }
  //
  //

  val lrcAddress: String = ""
  val data: Bitparser = Bitparser("")
  var dataOffset: Int = 0
  var tableOffset: Int = 0

  private def assembleOrder(): RawOrder = {
    val version = this.nextUint16.toString
    val owner = this.nextAddress
    val tokenS = this.nextAddress
    val tokenB = this.nextAddress
    val amountS = Numeric.toHexString(this.nextUint.toByteArray)
    val amountB = Numeric.toHexString(this.nextUint.toByteArray)
    val validSince = this.nextUint32
    // todo
    val tokenSpendableS = "" //this.spend
    val tokenSpendableFee = "" //this.spend
    val dualAuthAddr = this.nextAddress
    val broker = this.nextAddress
    val orderInterceptor = this.nextAddress
    val walletAddr = this.nextAddress
    val validUntil = this.nextUint32
    val sig = this.nextBytes
    val dualAuthSig = this.nextBytes
    val allOrNone = this.nextUint16 > 0
    val feeToken = this.nextAddress
    val feeAmount = Numeric.toHexString(this.nextUint.toByteArray)
    val feePercentage = this.nextUint16
    val waiveFeePercentage = this.toInt16(this.nextUint16)
    val tokenSFeePercentage = this.nextUint16
    val tokenBFeePercentage = this.nextUint16
    val tokenRecipient = this.nextAddress
    val walletSplitPercentage = this.nextUint16

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

  // todo: 该合约函数内容存疑，貌似啥也没干
  private def validateSpendables(orders: Seq[RawOrder]): Unit = {
    //    val ownerTokens = mutable.HashMap.empty[String, String]
    //
    //    orders.map(rawOrder => {
    //      val order = rawOrder.getEssential
    //
    //      val tokensKey = (order.owner + order.tokenS).toLowerCase
    //      if (!ownerTokens.contains(tokensKey)) {
    //        ownerTokens += tokensKey -> rawOrder.tokenSpendableS
    //      }
    //      assert(safeEquals(rawOrder.tokenSpendableS, ownerTokens.get(tokensKey).get), "Spendable for tokenS should match")
    //    })
  }

  private def nextAddress: String = {
    val offset = this.getNextOffset * 4
    if (offset != 0) {
      this.data.extractAddress(this.dataOffset + offset)
    } else {
      ""
    }
  }

  private def nextUint: BigInt = {
    val offset = this.getNextOffset * 4
    if (offset != 0) {
      this.data.extractUint(this.dataOffset + offset)
    } else {
      BigInt(0)
    }
  }

  private def nextUint16: Int = {
    this.getNextOffset
  }

  private def nextUint32: Int = {
    val offset = this.getNextOffset * 4
    if (offset != 0) {
      this.data.extractUint32(this.dataOffset + offset)
    } else {
      0
    }
  }

  private def nextBytes: String = {
    val offset = this.getNextOffset * 4
    if (offset != 0) {
      val len = this.data.extractUint(this.dataOffset + offset).intValue()
      Numeric.toHexString(this.data.extractBytesX(this.dataOffset + offset + 32, len))
    } else {
      ""
    }
  }

  private def toInt16(x: BigInt): Int = {
    x.intValue()
  }

  private def getNextOffset: Int = {
    val offset = this.data.extractUint16(this.tableOffset)
    this.tableOffset += 2
    this.tableOffset
  }
}
