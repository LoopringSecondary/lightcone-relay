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
import org.loopring.lightcone.lib.solidity.Abi
import org.loopring.lightcone.proto.block_chain_event.{OrderFilled, RingMined, SubmitRing, TxHeader}
import org.loopring.lightcone.proto.eth_jsonrpc.Log
import org.loopring.lightcone.proto.order.{Order, RawOrder}
import org.loopring.lightcone.proto.ring.Ring

class LoopringAbi @Inject()(val config: Config) extends ContractAbi {

  val FN_SUBMIT_RING = "submitRing"
  val EN_RINGMINED = "RingMined"

  override def abi: Abi = Abi.fromJson(config.getString("abi.impl"))
  override val supportedFunctions: Seq[String] = Seq(
    FN_SUBMIT_RING,
  )
  override val supportedEvents: Seq[String] = Seq(
    EN_RINGMINED,
  )
  override val sigFuncMap: Map[String, Abi.Function] = super.sigFuncMap
  override val sigEvtMap: Map[String, Abi.Event] = super.sigEvtMap
  override val nameFuncMap: Map[String, Abi.Function] = super.nameFuncMap
  override val nameEvtMap: Map[String, Abi.Event] = super.nameEvtMap

  def decodeInputAndAssemble(input: String, header: TxHeader): Seq[Any] = {
    val res = decodeInput(input)
    res.name match {
      case FN_SUBMIT_RING => Seq(assembleSubmitRing(res.list, header))
      case _ => Seq()
    }
  }

  def decodeLogAndAssemble(log: Log, header: TxHeader): Seq[Any] = {
    val res = decodeLog(log)
    res.name match {
      case EN_RINGMINED => Seq(assembleRingmined(res.list, header))
      case _ => Seq()
    }
  }

  def assembleSubmitRing(list: Seq[Any], header: TxHeader): SubmitRing = {
    if (list.length != 9) {
      throw new Exception("length of ring invalid")
    }

    val addressList = list(0) match {
      case arr: Array[Object] => {
        arr.map(sub => sub match {
          case son: Array[Object] => son.map(javaObj2Hex)
          case _ => throw new Exception("submitRing sub addresses type error")
        })
      }
      case _ => throw new Exception("submitRing address type error")
    }

    val bigintList = list(1) match {
      case arr: Array[Object] => {
        arr.map(sub => sub match {
          case son: Array[Object] => son.map(javaObj2Bigint)
          case _ => throw new Exception("submitRing sub bigintArgs type error")
        })
      }
      case _ => throw new Exception("submitRing bigintArgs type error")
    }

    val uintArgList = list(2) match {
      case arr: Array[Object] => {
        arr.map(sub => sub match {
          case son: Array[Object] => son.map(javaObj2Bigint)
          case _ => throw new Exception("submitRing sub uintArgs type error")
        })
      }
      case _ => throw new Exception("submitRing uintArgs type error")
    }

    val buyNoMoreThanAmountBList = list(3) match {
      case arr: Array[Object] => arr.map(javaObj2Boolean)
      case _ => throw new Exception("submitRing buyNoMoreThanAmountB type error")
    }

    val vList = list(4) match {
      case arr: Array[Object] => arr.map(javaObj2Bigint)
      case _ => throw new Exception("submitRing vlist type error")
    }

    val rList = list(5) match {
      case arr: Array[Object] => arr.map(javaObj2Hex)
      case _ => throw new Exception("submitRing rlist type error")
    }

    val sList = list(6) match {
      case arr: Array[Object] => arr.map(javaObj2Hex)
      case _ => throw new Exception("submitRing slist type error")
    }

    val feeReceipt = scalaAny2Hex(list(7))

    val feeSelection = scalaAny2Bigint(list(8))

    var raworders: Seq[RawOrder] = Seq()
    for (i <- 0 to 1) {
      val subAddrList = addressList(i)
      val subBigintList = bigintList(i)

      raworders +:= RawOrder()
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

    val maker = Order().withRawOrder(raworders(0).withTokenB(addressList(1)(1)))
    val taker = Order().withRawOrder(raworders(1).withTokenB(addressList(0)(1)))

    val ring = Ring().withOrders(Seq(maker, taker))
      .withFeeReceipt(feeReceipt)
      .withFeeSelection(feeSelection.intValue())

    // todo: delete after debug and test
    println(ring.toProtoString)

    SubmitRing().withRing(ring).withTxHeader(header)
  }

  // todo: safeBig处理负数还是有点问题
  def assembleRingmined(list: Seq[Any], header: TxHeader): RingMined = {

    if (list.length != 5) {
      throw new Exception("length of decoded ringmined invalid")
    }

    val ringindex = scalaAny2Bigint(list(0))
    val ringhash = scalaAny2Hex(list(1))
    val miner = scalaAny2Hex(list(2))
    val feeReceipt = scalaAny2Hex(list(3))
    val orderinfoList = list(4) match {
      case arr: Array[Object] => arr.map(javaObj2Hex)
      case _ => throw new Exception("ringmined orderinfo list type error")
    }

    require(orderinfoList.length.equals(14))

    var fills: Seq[OrderFilled] = Seq()

    val length = orderinfoList.length / 7
    for (i <- 0 to (length - 1)) {
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

      fills +:= OrderFilled()
        .withRingHash(ringhash)
        .withRingIndex(ringindex.toString)
        .withFillIndex(i)
        .withOrderHash(orderinfoList(orderhashIdx))
        .withOwner(orderinfoList(start + 1))
        .withPreOrderHash(orderinfoList(preOrderhashIdx))
        .withNextOrderHash(orderinfoList(nxtOrderhashIdx))
        .withTokenS(orderinfoList(start + 2))
        .withTokenB(orderinfoList(nxtOrderhashIdx + 2))
        .withAmountS(orderinfoList(start + 3))
        .withAmountB(orderinfoList(nxtOrderhashIdx + 3))
        .withLrcReward(orderinfoList(start + 4))
        .withLrcFee(lrcFee.toString)
        .withSplitS(splitS.toString)
        .withSplitB(splitB.toString)
    }

    val ring = RingMined()
      .withFills(fills)
      .withTxHeader(header)

    println(ring.toProtoString)

    ring
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
    val bytes = (1 to 32).map(_ => Byte.MaxValue).toArray
    new BigInteger(bytes)
  }
}
