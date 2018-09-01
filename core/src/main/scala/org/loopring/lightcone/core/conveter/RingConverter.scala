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

package org.loopring.lightcone.core.conveter

import com.google.protobuf.ByteString
import org.loopring.lightcone.proto.order.RawOrder
import org.loopring.lightcone.proto.ring.Ring

class RingConverter() extends ContractConverter[Ring] with TypeConverter {

  def convert(list: Seq[Any]): Ring = {
    if (list.length != 9) {
      throw new Exception("length of ring invalid")
    }

    val addressList = list(0) match {
      case arr: Array[Object] => {
        arr.map(sub => sub match {
          case son: Array[Object] => son.map(javaObj2Hex(_))
          case _ => throw new Exception("submitRing sub addresses type error")
        })
      }
      case _ => throw new Exception("submitRing address type error")
    }

    val bigintList = list(1) match {
      case arr: Array[Object] => {
        arr.map(sub => sub match {
          case son: Array[Object] => son.map(javaObj2Bigint(_))
          case _ => throw new Exception("submitRing sub bigintArgs type error")
        })
      }
      case _ => throw new Exception("submitRing bigintArgs type error")
    }

    val uintArgList = list(2) match {
      case arr: Array[Object] => {
        arr.map(sub => sub match {
          case son: Array[Object] => son.map(javaObj2Bigint(_))
          case _ => throw new Exception("submitRing sub uintArgs type error")
        })
      }
      case _ => throw new Exception("submitRing uintArgs type error")
    }

    val buyNoMoreThanAmountBList = list(3) match {
      case arr: Array[Object] => arr.map(javaObj2Boolean(_))
      case _ => throw new Exception("submitRing buyNoMoreThanAmountB type error")
    }

    val vList = list(4) match {
      case arr: Array[Object] => arr.map(javaObj2Bigint(_))
      case _ => throw new Exception("submitRing vlist type error")
    }

    val rList = list(5) match {
      case arr: Array[Object] => arr.map(javaObj2Hex(_))
      case _ => throw new Exception("submitRing rlist type error")
    }

    val sList = list(6) match {
      case arr: Array[Object] => arr.map(javaObj2Hex(_))
      case _ => throw new Exception("submitRing slist type error")
    }

    val feeReceipt = scalaAny2Hex(list(7))

    val feeSelection = scalaAny2Bigint(list(8))

    val maker = RawOrder()
      .withOwner(ByteString.copyFrom(addressList(0)(0).getBytes()))
      .withTokenS(ByteString.copyFrom(addressList(0)(1).getBytes()))
      .withTokenB(ByteString.copyFrom(addressList(1)(1).getBytes()))
      .withWalletAddress(ByteString.copyFrom(addressList(0)(2).getBytes()))
      .withAuthAddr(ByteString.copyFrom(addressList(0)(3).getBytes()))
      .withAmountS(ByteString.copyFrom(bigintList(0)(0).toByteArray))
      .withAmountB(ByteString.copyFrom(bigintList(0)(1).toByteArray))
      .withValidSince(bigintList(0)(2).intValue().toLong)
      .withValidUntil(bigintList(0)(3).intValue().toLong)
      .withLrcFee(ByteString.copyFrom(bigintList(0)(4).toByteArray))
      .withMarginSplitPercentage(uintArgList(0)(0).doubleValue())
      .withBuyNoMoreThanAmountB(buyNoMoreThanAmountBList(0))
      .withV(vList(0).intValue())
      .withR(ByteString.copyFrom(rList(0).getBytes()))
      .withS(ByteString.copyFrom(sList(0).getBytes()))

    val taker = RawOrder()
      .withOwner(ByteString.copyFrom(addressList(1)(0).getBytes()))
      .withTokenS(ByteString.copyFrom(addressList(1)(1).getBytes()))
      .withTokenB(ByteString.copyFrom(addressList(0)(1).getBytes()))
      .withWalletAddress(ByteString.copyFrom(addressList(1)(2).getBytes()))
      .withAuthAddr(ByteString.copyFrom(addressList(1)(3).getBytes()))
      .withAmountS(ByteString.copyFrom(bigintList(1)(0).toByteArray))
      .withAmountB(ByteString.copyFrom(bigintList(1)(1).toByteArray))
      .withValidSince(bigintList(1)(2).intValue().toLong)
      .withValidUntil(bigintList(1)(3).intValue().toLong)
      .withLrcFee(ByteString.copyFrom(bigintList(1)(4).toByteArray))
      .withMarginSplitPercentage(uintArgList(1)(0).doubleValue())
      .withBuyNoMoreThanAmountB(buyNoMoreThanAmountBList(1))
      .withV(vList(1).intValue())
      .withR(ByteString.copyFrom(rList(1).getBytes()))
      .withS(ByteString.copyFrom(sList(1).getBytes()))

    Ring().withOrders(Seq(maker, taker))
      .withFeeReceipt(feeReceipt)
      .withFeeSelection(feeSelection.intValue())
  }
}
