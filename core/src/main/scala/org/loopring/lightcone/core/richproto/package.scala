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

package org.loopring.lightcone.core

import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.order.{ Order, RawOrder, SoftCancelSign }
import org.loopring.lightcone.proto.ring.{ Ring, Rings }
import org.web3j.crypto.{ Hash ⇒ web3Hash, _ }
import org.web3j.utils.Numeric

package object richproto {

  val ethereumPrefix = "\u0019Ethereum Signed Message:\n"

  implicit class RichRawOrder(rawOrder: RawOrder) {

    def getSignerAddr(): String = {
      val orderHash = Numeric.hexStringToByteArray(rawOrder.hash)
      val hash = web3Hash.sha3((ethereumPrefix + orderHash.length).getBytes() ++ orderHash)
      //      val publicKey =
      //        Sign.recoverFromSignature(
      //        (rawOrder.v - 27).toByte,
      //        new ECDSASignature(rawOrder.r.asBigInteger, rawOrder.s.asBigInteger),
      //        hash
      //      )
      //      Keys.getAddress(publicKey)
      "0x"
    }

    def getHash(): String = {
      val data = Array[Byte]()
        .addUint256(rawOrder.amountS.asBigInteger)
        .addUint256(rawOrder.amountB.asBigInteger)
        .addUint256(rawOrder.feeAmount.asBigInteger)
        .addUint256(BigInt(rawOrder.validSince).bigInteger)
        .addUint256(BigInt(rawOrder.validUntil).bigInteger)
        .addAddress(rawOrder.owner)
        .addAddress(rawOrder.tokenS)
        .addAddress(rawOrder.tokenB)
        .addAddress(rawOrder.dualAuthAddress)
        .addAddress("0x0")
        .addAddress("0x0")
        .addAddress(rawOrder.wallet)
        .addAddress(rawOrder.tokenRecipient)
        .addAddress(rawOrder.feeToken)
        .addUint16(BigInt(rawOrder.walletSplitPercentage).bigInteger)
        .addUint16(BigInt(rawOrder.feePercentage).bigInteger)
        .addUint16(BigInt(rawOrder.tokenSFeePercentage).bigInteger)
        .addUint16(BigInt(rawOrder.tokenBFeePercentage).bigInteger)
        .addBoolean(rawOrder.allOrNone)

      Numeric.toHexString(web3Hash.sha3(data))
    }
  }

  implicit class RichOrder(order: Order) {
    //todo:protocol 2.0 逻辑更改，需要重新计算
    def getRemained(): (Rational, Rational) = {
      val remainedAmountS = Rational(
        order.rawOrder.get.amountS.asBigInt - (
          order.dealtAmountS.asBigInt +
          order.cancelledAmountS.asBigInt +
          order.splitAmountS.asBigInt
        )
      )
      val remainedAmountB = Rational(
        order.rawOrder.get.amountB.asBigInt - (
          order.dealtAmountS.asBigInt +
          order.cancelledAmountB.asBigInt +
          order.splitAmountB.asBigInt
        )
      )
      (remainedAmountS, remainedAmountB)
    }

    def sellPrice(): Rational = {
      Rational(order.rawOrder.get.amountS.asBigInt, order.rawOrder.get.amountB.asBigInt)
    }

    def buyPrice(): Rational = {
      Rational(order.rawOrder.get.amountB.asBigInt, order.rawOrder.get.amountS.asBigInt)
    }

  }

  implicit class RichRings(rings: Rings) {

    def getHash(): String = {
      val data = rings.orders.foldLeft(Array[Byte]()) {
        (res, order) ⇒
          res.addHex(order.hash)
            .addUint16(BigInt(order.waiveFeePercentage).bigInteger)
      }
      Numeric.toHexString(web3Hash.sha3(data))
    }

  }

  implicit class RichSoftCancelSign(sign: SoftCancelSign) {
    def getSignerAddr(): String = {
      val signHash = Numeric.hexStringToByteArray(getHash())
      val hash = web3Hash.sha3((ethereumPrefix + signHash.length).getBytes() ++ signHash)
      val publicKey = Sign.recoverFromSignature(
        (sign.v - 27).toByte,
        new ECDSASignature(sign.r.asBigInteger, sign.s.asBigInteger),
        hash
      )
      Keys.getAddress(publicKey)
    }

    def getHash(): String = {
      val data = sign.timestamp.toString.getBytes
      Numeric.toHexString(web3Hash.sha3(data))
    }
  }

}
