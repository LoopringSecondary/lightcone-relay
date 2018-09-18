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

import java.util.concurrent.ConcurrentHashMap

import akka.pattern._
import akka.util.Timeout
import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.balance._
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.proto.ring._
import org.loopring.lightcone.core.richproto._

import scala.collection.JavaConverters._
import scala.collection.concurrent
import scala.concurrent.{ ExecutionContext, Future }

class RingEvaluatorImpl(
    submitterAddress: String = "",
    lrcAddress: String,
    walletSplit: Rational = Rational(8, 10),
    gasUsedOfOrders: Map[Int, Int] = Map(2 -> 400000, 3 -> 500000, 4 -> 600000)
)(
    implicit
    ec: ExecutionContext, timeout: Timeout
)
  extends RingEvaluator {

  var avaliableAmounts: concurrent.Map[String, BigInt] =
    new ConcurrentHashMap[String, BigInt]().asScala

  var orderFillAmount = Map.empty[String, BigInt]

  def getRingCadidatesToSettle(candidates: RingCandidates): Future[Seq[RingCandidate]] =
    for {
      _ ← Future.successful(0)
      //订单已成交量
      orderFillAmount = Map[String, BigInt]()
      //账户可用金额等
      avaliableAmounts = new ConcurrentHashMap[String, BigInt]() asScala

      ringCandidates = candidates.rings
        .map(r ⇒ Some(RingCandidate(rawRing = r)))

      ringsToSettle ← getRingCadidatesToSettle(Seq(), ringCandidates)
        .mapTo[Seq[Option[RingCandidate]]]
    } yield ringsToSettle.filter(_.nonEmpty).map(_.get)

  //  @tailrec
  private def getRingCadidatesToSettle(
    candidatesForSubmit: Seq[Option[RingCandidate]],
    candidates: Seq[Option[RingCandidate]]
  ) =
    for {
      ringCandidates ← Future.sequence(candidates
        .filter(_.nonEmpty)
        .map(c ⇒ generateRingCandidate(c.get.rawRing)))

      //    //todo:相同地址的，需要根据余额再次计算成交量等，否则第二笔可能成交量与收益不足
      //    (candidatesForSubmit1, candidatesComputeAgain) = (ringCandidates, Seq[Option[RingCandidate]]())
      //    if (candidatesComputeAgain.size <= 0) {
      //      candidatesForSubmit ++ candidatesForSubmit1
      //    } else {
      //    getRingForSubmit(candidatesForSubmit ++ candidatesForSubmit1, candidatesComputeAgain)
      //    }
    } yield ringCandidates

  //余额以及授权金额
  private def getAvailableAmount(
    address: String,
    token: String,
    delegate: String
  ): Future[BigInt] =
    for {
      amount ← if (avaliableAmounts.contains(address))
        Future.successful(avaliableAmounts.getOrElse(address, BigInt(0)))
      else
        for {
          resp ← (
            Routers.balanceManager ? GetBalanceAndAllowanceReq(
              address = address,
              tokens = Seq(token),
              delegates = Seq(delegate)
            )
          ).mapTo[GetBalanceAndAllowanceResp]
          x = resp.balances.head.amount.asBigInt
          y = resp.allowances.head.tokenAmounts.head.amount.asBigInt
          min = x min y
        } yield min
    } yield amount

  private def priceReduceRate(ring: Ring): Rational = {
    val priceMul = ring.orders.map { order ⇒
      val rawOrder = order.rawOrder.get
      Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    }.reduceLeft(_ * _)

    val root = priceMul.pow(Rational(1, ring.orders.size))
    val reduceRate = Rational(root)
    Rational(1) / reduceRate
  }

  private def checkRing(ring: Ring) = {
    ring.orders.map(_.rawOrder.isDefined).reduceLeft(_ && _)
  }

  def generateRingCandidate(ring: Ring) = for {
    res ← if (!checkRing(ring)) {
      Future.successful(None)
    } else {
      for {
        reduceRate ← Future.successful { priceReduceRate(ring) }
        orderFillsStep1 ← Future.sequence(ring.orders.map {
          order ⇒
            for {
              rawOrder ← Future.successful(order.rawOrder.get)
              amountS = rawOrder.amountS.asBigInt
              rateAmountS = Rational(amountS) * reduceRate
              (fillAmountS, fillAmountB, sPrice) ← computeFillAmountStep1(order, reduceRate)
            } yield OrderFill(
              rawOrder,
              sPrice,
              rateAmountS,
              fillAmountS,
              fillAmountB,
              reduceRate
            )
        }).mapTo[Seq[OrderFill]]

        orderFillsStep2 = computeFillAmountStep2(orderFillsStep1)

        orderFillsSeq ← Future.sequence(orderFillsStep2.map {
          orderFill ⇒
            for {
              (feeSelection, receivedFiat) ← computeFeeOfOrder(orderFill)
              x = (
                orderFill.rawOrder.hash,
                orderFill.copy(
                  feeSelection = feeSelection,
                  receivedFiat = receivedFiat
                )
              )
            } yield x
        }).mapTo[Seq[(String, OrderFill)]]

        orderFillsMap = orderFillsSeq.toMap
        ringReceivedFiat = orderFillsMap.foldLeft(Rational(0))(_ + _._2.receivedFiat)
        gasPrice = Rational(1) //todo:fiat
        gasFiat = Rational(gasUsedOfOrders(orderFillsStep2.size)) * gasPrice

        result = Some(RingCandidate(
          rawRing = ring,
          receivedFiat = ringReceivedFiat - gasFiat,
          orderFills = orderFillsMap
        ))
      } yield result
    }
  } yield res

  private def computeFillAmountStep1(order: Order, reduceRate: Rational) = for {
    rawOrder ← Future.successful(order.rawOrder.get)
    availableAmountBig ← getAvailableAmount(
      rawOrder.owner,
      rawOrder.tokenS,
      rawOrder.delegateAddress
    )

    availableAmount = Rational(availableAmountBig)

    sPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt) * reduceRate

    (remainedAmountS, remainedAmountB) = order.getRemained()

    (fillAmountS, fillAmountB) = if (rawOrder.buyNoMoreThanAmountB) {
      val availableAmountB = remainedAmountB
      val availableAmountS = availableAmountB * sPrice
      (availableAmountS, availableAmountB)
    } else {
      val availableAmountS = remainedAmountS min availableAmount
      val availableAmountB = availableAmountS / sPrice
      (availableAmountS, availableAmountB)
    }
  } yield (fillAmountS, fillAmountB, sPrice)

  private def computeFillAmountStep2(orderFills: Seq[OrderFill]) = {
    var minVolumeIdx = 0
    var orderFillsRes = Seq[OrderFill](orderFills(minVolumeIdx))

    for (idx ← (0 until minVolumeIdx).reverse) {
      val fillAmountB = orderFills(idx + 1).fillAmountS
      val fillAmountS = fillAmountB * orderFills(idx).sPrice
      val fill1 = orderFills(idx).copy(fillAmountS = fillAmountS, fillAmountB = fillAmountB)
      orderFillsRes = fill1 +: orderFillsRes
    }

    for (idx ← minVolumeIdx + 1 to orderFills.size) {
      val fillAmountS = orderFills(idx - 1).fillAmountB
      val fillAmountB = fillAmountS / orderFills(idx).sPrice
      val fill1 = orderFills(idx).copy(fillAmountS = fillAmountS, fillAmountB = fillAmountB)
      orderFillsRes = orderFillsRes :+ fill1
    }

    orderFillsRes
  }

  private def computeFeeOfOrder(orderFill: OrderFill): Future[(Byte, Rational)] =
    for {
      submitterLrcAmount ← getAvailableAmount(
        submitterAddress,
        lrcAddress,
        orderFill.rawOrder.delegateAddress
      )

      splitPercentage = if (orderFill.rawOrder.marginSplitPercentage > 100) {
        Rational(1)
      } else {
        Rational(orderFill.rawOrder.marginSplitPercentage, 100)
      }

      savingFiatReceived = if (orderFill.rawOrder.buyNoMoreThanAmountB) {
        var savingAmountS = orderFill.fillAmountB * orderFill.sPrice - orderFill.fillAmountS
        splitPercentage * savingAmountS //todo:transfer to fiat amount
      } else {
        var savingAmountB = orderFill.fillAmountB - orderFill.fillAmountB * orderFill.reduceRate
        splitPercentage * savingAmountB //todo:
      }

      fillRate = orderFill.fillAmountS / Rational(orderFill.rawOrder.amountS.asBigInt)
      lrcFee = fillRate * Rational(orderFill.rawOrder.lrcFee.asBigInt)
      lrcFiatReceived = lrcFee //todo:transfer to fiat amount

      (feeSelection, receivedFiat) = if (lrcFiatReceived.signum == 0 ||
        lrcFiatReceived * Rational(2) < savingFiatReceived) {
        (1.toByte, savingFiatReceived)
      } else {
        (0.toByte, lrcFiatReceived)
      }
    } yield (feeSelection, receivedFiat * walletSplit)

}
