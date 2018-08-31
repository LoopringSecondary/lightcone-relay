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

/*
* func (m *SubmitRingMethodInputs) ConvertDown(protocol, delegate common.Address) (*types.SubmitRingMethodEvent, error) {
   var (
      list  []types.Order
      event types.SubmitRingMethodEvent
   )

   length := len(m.AddressList)
   vrsLength := 2 * length

   orderLengthInvalid := length < 2
   argLengthInvalid := length != len(m.UintArgsList) || length != len(m.Uint8ArgsList)
   vrsLengthInvalid := vrsLength != len(m.VList) || vrsLength != len(m.RList) || vrsLength != len(m.SList)
   if orderLengthInvalid || argLengthInvalid || vrsLengthInvalid {
      return nil, fmt.Errorf("submitRing method unpack error:orders length invalid")
   }

   for i := 0; i < length; i++ {
      var order types.Order

      order.Protocol = protocol
      order.DelegateAddress = delegate
      order.Owner = m.AddressList[i][0]
      order.TokenS = m.AddressList[i][1]
      if i == length-1 {
         order.TokenB = m.AddressList[0][1]
      } else {
         order.TokenB = m.AddressList[i+1][1]
      }
      order.WalletAddress = m.AddressList[i][2]
      order.AuthAddr = m.AddressList[i][3]

      order.AmountS = m.UintArgsList[i][0]
      order.AmountB = m.UintArgsList[i][1]
      order.ValidSince = m.UintArgsList[i][2]
      order.ValidUntil = m.UintArgsList[i][3]
      order.LrcFee = m.UintArgsList[i][4]
      // order.rateAmountS

      order.MarginSplitPercentage = m.Uint8ArgsList[i][0]

      order.BuyNoMoreThanAmountB = m.BuyNoMoreThanBList[i]

      order.V = m.VList[i]
      order.R = m.RList[i]
      order.S = m.SList[i]

      order.Hash = order.GenerateHash()
      list = append(list, order)
   }

   event.OrderList = list
   event.FeeReceipt = m.FeeRecipient
   event.FeeSelection = m.FeeSelections
   event.Err = ""

   return &event, nil
}
* */
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

    val ring = Ring().withOrders(Seq(maker, taker))
      .withFeeReceipt(feeReceipt)
      .withFeeSelection(feeSelection.intValue())

    println(s"------ ring is ${ring.toProtoString}")
    ring
  }
}
