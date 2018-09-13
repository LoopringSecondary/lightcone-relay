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

import java.math.BigInteger

import com.google.inject.Inject
import com.typesafe.config.Config
import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.lib.solidity.Abi
import org.loopring.lightcone.proto.block_chain_event._
import org.loopring.lightcone.proto.eth_jsonrpc.Log
import org.loopring.lightcone.proto.order.{ Order, RawOrder }
import org.loopring.lightcone.proto.ring.Ring

class LoopringAbi @Inject() (val config: Config) extends ContractAbi {

  val FN_SUBMIT_RING = "submitRing"
  val FN_CANCEL_ORDER = "cancelOrder"
  val FN_CUTOFF_ALL = "cancelAllOrders"
  val FN_CUTOFF_PAIR = "cancelAllOrdersByTradingPair"

  val EN_RING_MINED = "RingMined"
  val EN_ORDER_CANCELLED = "OrderCancelled"
  val EN_CUTOFF_ALL = "AllOrdersCancelled"
  val EN_CUTOFF_PAIR = "OrdersCancelled"

  override def abi: Abi = Abi.fromJson(getAbiResource("abi/loopring.json"))

  override def supportedFunctions: Seq[String] = Seq(
    FN_SUBMIT_RING, FN_CANCEL_ORDER, FN_CUTOFF_ALL, FN_CUTOFF_PAIR
  )
  override def supportedEvents: Seq[String] = Seq(
    EN_RING_MINED, EN_ORDER_CANCELLED, EN_CUTOFF_ALL, EN_CUTOFF_PAIR
  )

  def decodeInputAndAssemble(input: String, header: TxHeader): Seq[Any] = {
    val res = decodeInput(input)
    res.name match {
      case FN_SUBMIT_RING  ⇒ Seq(assembleSubmitRingFunction(res.list, header))
      case FN_CANCEL_ORDER ⇒ Seq(assembleCancelOrderFunction(res.list, header))
      case FN_CUTOFF_ALL   ⇒ Seq(assembleCutoffFunction(res.list, header))
      case FN_CUTOFF_PAIR  ⇒ Seq(assembleCutoffPairFunction(res.list, header))
      case _               ⇒ Seq()
    }
  }

  def decodeLogAndAssemble(log: Log, header: TxHeader): Seq[Any] = {
    val res = decodeLog(log)
    res.name match {
      case EN_RING_MINED      ⇒ Seq(assembleRingminedEvent(res.list, header))
      case EN_ORDER_CANCELLED ⇒ Seq(assembleOrderCancelledEvent(res.list, header))
      case EN_CUTOFF_ALL      ⇒ Seq(assembleCutoffEvent(res.list, header))
      case EN_CUTOFF_PAIR     ⇒ Seq(assembleCutoffPairEvent(res.list, header))
      case _                  ⇒ Seq()
    }
  }

  def assembleSubmitRingFunction(list: Seq[Any], header: TxHeader): SubmitRing = {
    if (list.length != 9) {
      throw new Exception("length of ring invalid")
    }

    val addressList = list(0) match {
      case arr: Array[Object] ⇒ {
        arr.map(sub ⇒ sub match {
          case son: Array[Object] ⇒ son.map(javaObj2Hex)
          case _                  ⇒ throw new Exception("submitRing sub addresses type error")
        })
      }
      case _ ⇒ throw new Exception("submitRing address type error")
    }

    val bigintList = list(1) match {
      case arr: Array[Object] ⇒ {
        arr.map(sub ⇒ sub match {
          case son: Array[Object] ⇒ son.map(javaObj2Bigint)
          case _                  ⇒ throw new Exception("submitRing sub bigintArgs type error")
        })
      }
      case _ ⇒ throw new Exception("submitRing bigintArgs type error")
    }

    val uintArgList = list(2) match {
      case arr: Array[Object] ⇒ {
        arr.map(sub ⇒ sub match {
          case son: Array[Object] ⇒ son.map(javaObj2Bigint)
          case _                  ⇒ throw new Exception("submitRing sub uintArgs type error")
        })
      }
      case _ ⇒ throw new Exception("submitRing uintArgs type error")
    }

    val buyNoMoreThanAmountBList = list(3) match {
      case arr: Array[Object] ⇒ arr.map(javaObj2Boolean)
      case _                  ⇒ throw new Exception("submitRing buyNoMoreThanAmountB type error")
    }

    val vList = list(4) match {
      case arr: Array[Object] ⇒ arr.map(javaObj2Bigint)
      case _                  ⇒ throw new Exception("submitRing vlist type error")
    }

    val rList = list(5) match {
      case arr: Array[Object] ⇒ arr.map(javaObj2Hex)
      case _                  ⇒ throw new Exception("submitRing rlist type error")
    }

    val sList = list(6) match {
      case arr: Array[Object] ⇒ arr.map(javaObj2Hex)
      case _                  ⇒ throw new Exception("submitRing slist type error")
    }

    val feeReceipt = scalaAny2Hex(list(7))
    val feeSelection = scalaAny2Bigint(list(8))
    val protocol = header.safeTo

    var raworders: Seq[RawOrder] = Seq()
    for (i ← 0 to 1) {
      val subAddrList = addressList(i)
      val subBigintList = bigintList(i)

      raworders +:= RawOrder()
        .withProtocol(protocol)
        .withOwner(subAddrList(0))
        .withTokenS(subAddrList(1))
        .withWalletAddress(subAddrList(2))
        .withAuthAddr(subAddrList(3))
        .withAmountS(subBigintList(0).toString)
        .withAmountB(subBigintList(1).toString)
        .withValidSince(subBigintList(2).bigInteger.longValue())
        .withValidUntil(subBigintList(3).bigInteger.longValue())
        .withLrcFee(subBigintList(4).toString())
        .withMarginSplitPercentage(uintArgList(i)(0).bigInteger.intValue())
        .withBuyNoMoreThanAmountB(buyNoMoreThanAmountBList(i))
        .withV(vList(i).intValue())
        .withR(rList(i))
        .withS(sList(i))
    }

    val maker = Order().withRawOrder(raworders(0).withTokenB(raworders(1).tokenS))
    val taker = Order().withRawOrder(raworders(1).withTokenB(raworders(0).tokenS))

    val ring = Ring().withOrders(Seq(maker, taker))
      .withFeeReceipt(feeReceipt)
      .withFeeSelection(feeSelection.intValue())

    // todo: delete after debug and test
    println(ring.toProtoString)

    SubmitRing().withRing(ring).withTxHeader(header)
  }

  // todo: safeBig处理负数还是有点问题
  def assembleRingminedEvent(list: Seq[Any], header: TxHeader): RingMined = {
    if (list.length != 5) {
      throw new Exception("length of decoded ringmined invalid")
    }

    val ringindex = scalaAny2Bigint(list(0))
    val ringhash = scalaAny2Hex(list(1))
    val miner = scalaAny2Hex(list(2))
    val feeReceipt = scalaAny2Hex(list(3))
    val orderinfoList = list(4) match {
      case arr: Array[Object] ⇒ arr.map(javaObj2Hex)
      case _                  ⇒ throw new Exception("ringmined orderinfo list type error")
    }

    require(orderinfoList.length.equals(14))

    var fills: Seq[OrderFilled] = Seq()

    val length = orderinfoList.length / 7
    for (i ← 0 to (length - 1)) {
      val start = i * 7

      val orderhashIdx = start
      val preOrderhashIdx = if (i.equals(0)) (length - 1) * 7 else (i - 1) * 7
      val nxtOrderhashIdx = if (i.equals(length - 1)) 0 else (i + 1) * 7

      val lrcFeeOrReward = orderinfoList(start + 5)
      val lrcFee = if (safeBig(lrcFeeOrReward.getBytes()).bigInteger.compareTo(BigInt(0)) > 0) {
        lrcFeeOrReward
      } else {
        BigInt(0).toString()
      }

      val split = orderinfoList(start + 6)
      val (splitS, splitB) = if (safeBig(split.getBytes()).bigInteger.compareTo(BigInt(0)) > 0) {
        (split, BigInt(0).toString())
      } else {
        (BigInt(0).toString(), split)
      }

      val owner = orderinfoList(start + 1).asAddress.toString
      val tokenS = orderinfoList(start + 2).asAddress.toString
      val tokenB = orderinfoList(nxtOrderhashIdx + 2).asAddress.toString

      fills +:= OrderFilled()
        .withRingHash(ringhash)
        .withRingIndex(ringindex.toString)
        .withFillIndex(i)
        .withOrderHash(orderinfoList(orderhashIdx))
        .withOwner(owner)
        .withPreOrderHash(orderinfoList(preOrderhashIdx))
        .withNextOrderHash(orderinfoList(nxtOrderhashIdx))
        .withTokenS(tokenS)
        .withTokenB(tokenB)
        .withAmountS(orderinfoList(start + 3).asBigInteger.toString)
        .withAmountB(orderinfoList(nxtOrderhashIdx + 3).asBigInteger.toString)
        .withLrcReward(orderinfoList(start + 4).asBigInteger.toString)
        .withLrcFee(lrcFee.asBigInteger.toString)
        .withSplitS(splitS.asBigInteger.toString)
        .withSplitB(splitB.asBigInteger.toString)
    }

    val ring = RingMined()
      .withFills(fills)
      .withTxHeader(header)

    println(ring.toProtoString)

    ring
  }

  def assembleCancelOrderFunction(list: Seq[Any], header: TxHeader): CancelOrder = {
    if (list.length != 7) {
      throw new Exception("length of decode cancelOrder function  invalid")
    }

    val addressList = list(0) match {
      case arr: Array[Object] if (arr.length.equals(5)) ⇒ arr.map(javaObj2Hex)
      case _ ⇒ throw new Exception("cancel order function addresslist type error")
    }

    val bigintList = list(1) match {
      case arr: Array[Object] if (arr.length.equals(6)) ⇒ arr.map(javaObj2Bigint)
      case _ ⇒ throw new Exception("cancel order function bigintList type error")
    }

    val order = RawOrder()
      .withProtocol(header.safeTo)
      .withOwner(addressList(0))
      .withTokenS(addressList(1))
      .withTokenB(addressList(2))
      .withWalletAddress(addressList(3))
      .withAuthAddr(addressList(4))
      .withAmountS(bigintList(0).toString)
      .withAmountB(bigintList(1).toString)
      .withValidSince(bigintList(2).bigInteger.longValue())
      .withValidUntil(bigintList(3).bigInteger.longValue())
      .withLrcFee(bigintList(4).toString())
      .withBuyNoMoreThanAmountB(scalaAny2Bool(list(2)))
      .withMarginSplitPercentage(scalaAny2Bigint(list(3)).intValue())
      .withV(scalaAny2Bigint(list(4)).intValue())
      .withS(scalaAny2Hex(list(5)))
      .withR(scalaAny2Hex(list(6)))

    val cancelAmount = bigintList(5).toString()

    val ret = CancelOrder()
      .withOrder(order)
      .withCancelAmount(cancelAmount)
      .withTxHeader(header)

    print(ret.toProtoString)
    ret
  }

  def assembleOrderCancelledEvent(list: Seq[Any], header: TxHeader): OrderCancelled = {
    if (list.length != 2) {
      throw new Exception("length of decode orderCancelled event invalid")
    }

    val ret = OrderCancelled()
      .withOrderHash(scalaAny2Hex(list(0)))
      .withAmount(scalaAny2Bigint(list(1)).toString())
      .withTxHeader(header)

    print(ret.toProtoString)
    ret
  }

  def assembleCutoffFunction(list: Seq[Any], header: TxHeader): Cutoff = {
    if (list.length != 1) {
      throw new Exception("length of decode cutoff function invalid")
    }

    val ret = Cutoff()
      .withOwner(header.from)
      .withCutoff(scalaAny2Bigint(list(0)).intValue())
      .withTxHeader(header)

    print(ret.toProtoString)

    ret
  }

  def assembleCutoffEvent(list: Seq[Any], header: TxHeader): Cutoff = {
    if (list.length != 2) {
      throw new Exception("length of decode cutoff event invalid")
    }

    val ret = Cutoff()
      .withOwner(header.from)
      .withCutoff(scalaAny2Bigint(list(1)).intValue())
      .withTxHeader(header)

    print(ret.toProtoString)
    ret
  }

  def assembleCutoffPairFunction(list: Seq[Any], header: TxHeader): CutoffPair = {
    if (list.length != 3) {
      throw new Exception("length of decode cutoff pair function invalid")
    }

    val ret = CutoffPair()
      .withOwner(header.from)
      .withToken1(scalaAny2Hex(list(0)))
      .withToken2(scalaAny2Hex(list(1)))
      .withCutoff(scalaAny2Bigint(list(2)).intValue())
      .withTxHeader(header)

    print(ret.toProtoString)

    ret
  }

  def assembleCutoffPairEvent(list: Seq[Any], header: TxHeader): CutoffPair = {
    if (list.length != 4) {
      throw new Exception("length of decode cutoff pair function invalid")
    }

    // o 为indexed == header.from

    val ret = CutoffPair()
      .withOwner(header.from)
      .withToken1(scalaAny2Hex(list(1)))
      .withToken2(scalaAny2Hex(list(2)))
      .withCutoff(scalaAny2Bigint(list(3)).intValue())
      .withTxHeader(header)

    print(ret.toProtoString)

    ret
  }

  // 对负数异或取反，这两个方法只有该文件用到
  def safeBig(bytes: Array[Byte]): BigInt = {
    if (bytes(0) > 128) {
      new BigInteger(bytes).xor(maxUint256).not()
    } else {
      BigInt(bytes)
    }
  }

  val maxUint256: BigInteger = {
    val bytes = (1 to 32).map(_ ⇒ Byte.MaxValue).toArray
    new BigInteger(bytes)
  }
}
