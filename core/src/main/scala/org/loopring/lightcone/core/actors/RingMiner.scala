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

package org.loopring.lightcone.core.actors

import akka.actor._
import akka.pattern._
import akka.util.Timeout
import com.google.protobuf.ByteString
import org.loopring.lightcone.core.actors.base._
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.ring._
import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.order.{ Order, RawOrder }
import scala.collection.mutable.Map
import scala.collection.immutable
import scala.concurrent.Future

object RingMiner
  extends base.Deployable[RingMinerSettings] {
  val name = "ring_miner"
  val isSingleton = true

  def props = Props(classOf[RingMiner]).withDispatcher("ring-dispatcher")

  def getCommon(s: RingMinerSettings) =
    base.CommonSettings(s.address, s.roles, 1)
}

class RingMiner(implicit timout: Timeout)
  extends RepeatedJobActor with Evaluator {

  import context.dispatcher
  var finders: Seq[ActorRef] = Seq.empty
  lazy val balanceManager = Routers.balanceManager
  lazy val ethereumAccessor = Routers.ethereumAccessor

  override def receive: Receive = super.receive orElse {
    case settings: RingMinerSettings =>
      finders = settings.marketIds.map { id => Routers.ringFinder(id) }
      initAndStartNextRound(settings.scheduleDelay)
  }

  def handleRepeatedJob() = for {
    resps <- Future.sequence(finders.map { _ ? GetRingCandidates() })
      .mapTo[Seq[RingCandidates]]
  } yield {
    purgeCacheState()
    val rings = resps.map(_.rings).flatten
    val ringCandidate = rings.map(generateRingCandidate)
  }

  def decideRingCandidates(ring: Seq[Ring]): NotifyRingSettlementDecisions = {
    NotifyRingSettlementDecisions()
  }
}

case class OrderFill(
  rawOrder: RawOrder,
  sPrice: Rational,
  rateAmountS: Rational,
  fillAmountS: Rational,
  fillAmountB: Rational,
  reduceRate: Rational,
  receivedFiat: Rational,
  feeSelection: Byte)

case class RingCandidate(ringCandidate: Ring, receivedFiat: Rational, submitter: ByteString, orderFills: immutable.Map[ByteString, OrderFill])

trait Evaluator {
  //不同订单数量的环路执行需要的gas数
  val gasUsedOfOrders = Map(2 -> 400000, 3 -> 500000, 4 -> 600000)
  //订单已成交量
  private var orderFillAmount = Map[ByteString, BigInt]()
  //账户可用金额等
  private var avaliableAmounts = Map[ByteString, Map[ByteString, BigInt]]() //todo:change it

  def purgeCacheState() = {
    orderFillAmount = Map[ByteString, BigInt]()
    avaliableAmounts = Map[ByteString, Map[ByteString, BigInt]]()
  }

  //余额以及授权金额
  private def getAvailableAmount(delegateAddress: ByteString)(address: ByteString) = {
    //    avaliableAmounts.getOrElseUpdate()
    val balance = BigInt(0)
    val allowance = BigInt(0)
    val availableAmount = balance min allowance

    availableAmount
  }

  private def priceReduceRate(ring: Ring): Rational = {
    val priceMul = ring.orders.map { order =>
      val rawOrder = order.rawOrder.get
      //todo:
      Rational(BigInt(10), BigInt(2))
    }.reduceLeft(_ * _)

    val root = priceMul.pow(Rational(BigInt(2), BigInt(1)))
    val reduceRate = Rational(root)
    Rational(BigInt(1), BigInt(1)) / reduceRate
  }

  private def checkRing(ring: Ring) = {
    val orderCheck = ring.orders.map(_.rawOrder.isDefined).reduceLeft(_ && _)

    orderCheck
  }

  def generateRingCandidate(ring: Ring): Option[RingCandidate] = {
    val check = checkRing(ring)
    if (!check)
      return None
    val reduceRate = priceReduceRate(ring)
    var orderFills = ring.orders.map { order =>
      val rawOrder = order.rawOrder.get
      val amountS = BigInt(rawOrder.amountS.toByteArray)
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
    val submitter = ByteString.EMPTY
    Some(RingCandidate(ring, ringReceivedFiat, submitter, orderFillsMap))
  }

  private def computeFillAmountStep1(rawOrder: RawOrder, reduceRate: Rational) = {
    //todo: amounts/amountb
    val sPrice = Rational(1) * reduceRate
    val avaliableAmount = BigInt(10)
    val remainedAmountS = Rational(1)
    val remainedAmountB = Rational(1)
    val (fillAmountS, fillAmountB) = if (rawOrder.buyNoMoreThanAmountB) {
      val availableAmountB = remainedAmountB
      val availableAmountS = availableAmountB * sPrice
      (availableAmountS, availableAmountB)
    } else {
      val availableAmountS = Rational(1) //todo:val availableAmountS = remainedAmountS min avaliableAmount
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
    val feeReceiptLrcAmount = getAvailableAmount(ByteString.EMPTY)(ByteString.EMPTY) //todo:
    val splitPercentage = if (orderFill.rawOrder.marginSplitPercentage > 100) {
      Rational(1)
    } else {
      Rational(orderFill.rawOrder.marginSplitPercentage, 100)
    }
    val savingFiatReceived = if (orderFill.rawOrder.buyNoMoreThanAmountB) {
      var savingAmountS = orderFill.fillAmountB * orderFill.sPrice - orderFill.fillAmountS
      splitPercentage * savingAmountS //todo:transfer to fait amount
    } else {
      var savingAmountB = orderFill.fillAmountB - orderFill.fillAmountB * orderFill.reduceRate
      splitPercentage * savingAmountB //todo:
    }
    val fillRate = orderFill.fillAmountS / Rational(1) //todo:Rational(orderFill.rawOrder.amountS)
    val lrcFee = fillRate * Rational(1) //todo:orderFill.rawOrder.lrcFee
    val lrcFiatReceived = lrcFee //todo:transfer to fait amount

    val gasFiat = Rational(1) //todo:
    val (feeSelection, receivedFiat) = if (lrcFiatReceived.signum == 0 || lrcFiatReceived * Rational(2) < savingFiatReceived) {
      (1.toByte, savingFiatReceived - gasFiat)
    } else {
      (0.toByte, lrcFiatReceived - gasFiat)
    }
    (feeSelection, receivedFiat * Rational(8, 10))
  }

}