/*

  Copyright 2017 Loopring Project Ltd (Loopring Foundation).

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

*/

package org.loopring.lightcone.core.utils

import org.loopring.lightcone.core.etypes._
import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.order.RawOrder
import org.loopring.lightcone.proto.ring.Ring

import scala.annotation.tailrec


case class OrderFill(
                      rawOrder: RawOrder,
                      sPrice: Rational,
                      rateAmountS: Rational,
                      fillAmountS: Rational,
                      fillAmountB: Rational,
                      reduceRate: Rational,
                      receivedFiat: Rational,
                      feeSelection: Byte)

case class RingCandidate(rawRing: Ring, receivedFiat: Rational = Rational(0), gasPrice: BigInt = BigInt(0), gasLimit: BigInt = BigInt(0), submitter: String = "", orderFills: Map[String, OrderFill] = Map())


class RingEvaluationUtil {
  var walletSplit = Rational(8, 10)
  //不同订单数量的环路执行需要的gas数
  val gasUsedOfOrders = Map(2 -> 400000, 3 -> 500000, 4 -> 600000)

  @tailrec
  final def getRingForSubmit(candidatesForSubmit: Seq[Option[RingCandidate]], candidates: Seq[Option[RingCandidate]]): Seq[Option[RingCandidate]] = {
    val ringCandidates = candidates
      .filter(_.nonEmpty)
      .map(c => generateRingCandidate(c.get.rawRing))

    //todo:相同地址的，需要根据余额再次计算成交量等，否则第二笔可能成交量与收益不足
    val (candidatesForSubmit1, candidatesComputeAgain) = (ringCandidates, Seq[Option[RingCandidate]]())

    if (candidatesComputeAgain.size <= 0) {
      candidatesForSubmit ++ candidatesForSubmit1
    } else {
      getRingForSubmit(candidatesForSubmit ++ candidatesForSubmit1, candidatesComputeAgain)
    }

  }

  //余额以及授权金额
  private def getAvailableAmount(address: String) = {
    BigInt(0)
  }

  private def priceReduceRate(ring: Ring): Rational = {
    val priceMul = ring.orders.map { order =>
      val rawOrder = order.rawOrder.get
      //todo:
      Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    }.reduceLeft(_ * _)

    val root = priceMul.pow(Rational(2))
    val reduceRate = Rational(root)
    Rational(1) / reduceRate
  }

  private def checkRing(ring: Ring) = {
    val orderCheck = ring.orders.map(_.rawOrder.isDefined).reduceLeft(_ && _)

    orderCheck
  }

  def generateRingCandidate(ring: Ring): Option[RingCandidate] = {
    if (!checkRing(ring))
      return None

    val reduceRate = priceReduceRate(ring)
    var orderFills = ring.orders.map { order =>
      val rawOrder = order.rawOrder.get
      val amountS = rawOrder.amountS.asBigInt
      val rateAmountS = Rational(amountS) * reduceRate
      val (fillAmountS, fillAmountB, sPrice) = computeFillAmountStep1(rawOrder, reduceRate)
      OrderFill(rawOrder, sPrice, rateAmountS, fillAmountS, fillAmountB, reduceRate, Rational(1), 0.toByte)
    }
    orderFills = computeFillAmountStep2(orderFills)
    val orderFillsMap = orderFills.map { orderFill =>
      val (feeSelection, receivedFiat) = computeFeeOfOrder(orderFill)
      (orderFill.rawOrder.hash, orderFill.copy(feeSelection = feeSelection, receivedFiat = receivedFiat))
    }.toMap

    val ringReceivedFiat = orderFillsMap.foldLeft(Rational(0))(_ + _._2.receivedFiat)
    val gasPrice = Rational(1) //todo:fiat
    val gasFiat = Rational(gasUsedOfOrders(orderFills.size)) * gasPrice
    val submitter = ""
    Some(RingCandidate(rawRing = ring, receivedFiat = ringReceivedFiat - gasFiat, submitter = submitter, orderFills = orderFillsMap))
  }

  private def computeFillAmountStep1(rawOrder: RawOrder, reduceRate: Rational) = {
    val sPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt) * reduceRate
    val availableAmount = Rational(getAvailableAmount(rawOrder.owner)) //todo: balance min allowance
    val remainedAmountS = Rational(1) //todo:
    val remainedAmountB = Rational(1)
    val (fillAmountS, fillAmountB) = if (rawOrder.buyNoMoreThanAmountB) {
      val availableAmountB = remainedAmountB
      val availableAmountS = availableAmountB * sPrice
      (availableAmountS, availableAmountB)
    } else {
      val availableAmountS = remainedAmountS min availableAmount
      val availableAmountB = availableAmountS / sPrice
      (availableAmountS, availableAmountB)
    }
    (fillAmountS, fillAmountB, sPrice)
  }

  private def computeFillAmountStep2(orderFills: Seq[OrderFill]) = {
    var minVolumeIdx = 0
    var orderFillsRes = Seq[OrderFill](orderFills(minVolumeIdx))
    for (idx <- (0 until minVolumeIdx).reverse) {
      val fillAmountB = orderFills(idx + 1).fillAmountS
      val fillAmountS = fillAmountB * orderFills(idx).sPrice
      val fill1 = orderFills(idx).copy(fillAmountS = fillAmountS, fillAmountB = fillAmountB)
      orderFillsRes = fill1 +: orderFillsRes
    }
    for (idx <- minVolumeIdx + 1 to orderFills.size) {
      val fillAmountS = orderFills(idx - 1).fillAmountB
      val fillAmountB = fillAmountS / orderFills(idx).sPrice
      val fill1 = orderFills(idx).copy(fillAmountS = fillAmountS, fillAmountB = fillAmountB)
      orderFillsRes = orderFillsRes :+ fill1
    }
    orderFillsRes
  }

  private def computeFeeOfOrder(orderFill: OrderFill) = {
    val feeReceiptLrcAmount = getAvailableAmount(orderFill.rawOrder.owner)
    val splitPercentage = if (orderFill.rawOrder.marginSplitPercentage > 100) {
      Rational(1)
    } else {
      Rational(orderFill.rawOrder.marginSplitPercentage, 100)
    }
    val savingFiatReceived = if (orderFill.rawOrder.buyNoMoreThanAmountB) {
      var savingAmountS = orderFill.fillAmountB * orderFill.sPrice - orderFill.fillAmountS
      splitPercentage * savingAmountS //todo:transfer to fiat amount
    } else {
      var savingAmountB = orderFill.fillAmountB - orderFill.fillAmountB * orderFill.reduceRate
      splitPercentage * savingAmountB //todo:
    }
    val fillRate = orderFill.fillAmountS / Rational(orderFill.rawOrder.amountS.asBigInt)
    val lrcFee = fillRate * Rational(orderFill.rawOrder.lrcFee.asBigInt)
    val lrcFiatReceived = lrcFee //todo:transfer to fiat amount

    val (feeSelection, receivedFiat) = if (lrcFiatReceived.signum == 0 ||
      lrcFiatReceived * Rational(2) < savingFiatReceived) {
      (1.toByte, savingFiatReceived)
    } else {
      (0.toByte, lrcFiatReceived)
    }
    (feeSelection, receivedFiat * walletSplit)
  }

}
