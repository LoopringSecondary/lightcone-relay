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

package org.loopring.lightcone.core.assemble

import org.loopring.lightcone.proto.block_chain_event.FullTransaction
import org.loopring.lightcone.proto.order.{ Order, RawOrder }
import org.loopring.lightcone.proto.ring.Ring

class AssembleRingImpl() extends Assembler[Ring] {

  def convert(list: Seq[Any]): Ring = {
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

    ring
  }

  def txAddHeader(src: Ring, tx: FullTransaction): Ring = ???
  def txFullFilled(src: Ring, tx: FullTransaction): Ring = ???
}
