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

import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.order.Order
import org.loopring.lightcone.lib.etypes._
import org.web3j.crypto._
import org.web3j.utils.Numeric

package object richproto {
  implicit class RichOrder(order: Order) {

    def getRemained(): (Rational, Rational) = {
      if (order.rawOrder.get.buyNoMoreThanAmountB) {
        val sellPrice = order.sellPrice()
        val remainedAmountB = Rational(
          order.rawOrder.get.amountB.asBigInt - (
            order.dealtAmountS.asBigInt +
            order.cancelledAmountB.asBigInt +
            order.splitAmountB.asBigInt
          )
        )
        (remainedAmountB * sellPrice, remainedAmountB)
      } else {
        val remainedAmountS = Rational(
          order.rawOrder.get.amountS.asBigInt - (
            order.dealtAmountS.asBigInt +
            order.cancelledAmountS.asBigInt +
            order.splitAmountS.asBigInt
          )
        )
        val buyPrice = order.buyPrice()
        (remainedAmountS, remainedAmountS * buyPrice)
      }
    }

    def getSignerAddr(): String = {
      val rawOrder = order.rawOrder.get
      val orderHash = Numeric.hexStringToByteArray(order.rawOrder.get.hash)
      val hash = Hash.sha3(("\u0019Ethereum Signed Message:\n" + orderHash.length).getBytes() ++ orderHash)
      val publicKey = Sign.recoverFromSignature(
        (rawOrder.v - 27).toByte,
        new ECDSASignature(rawOrder.r.asBigInteger, rawOrder.s.asBigInteger),
        hash)
      Keys.getAddress(publicKey)
    }

    def sellPrice(): Rational = {
      Rational(order.rawOrder.get.amountS.asBigInt, order.rawOrder.get.amountB.asBigInt)
    }

    def buyPrice(): Rational = {
      Rational(order.rawOrder.get.amountB.asBigInt, order.rawOrder.get.amountS.asBigInt)
    }

    def getHash():String = {
      val rawOrder = order.rawOrder.get
      val buyNoMoreThanB = if (rawOrder.buyNoMoreThanAmountB) 1 else 0

      val data = Numeric.hexStringToByteArray(rawOrder.delegateAddress) ++
        Numeric.hexStringToByteArray(rawOrder.owner) ++
        Numeric.hexStringToByteArray(rawOrder.tokenS) ++
        Numeric.hexStringToByteArray(rawOrder.tokenB) ++
        Numeric.hexStringToByteArray(rawOrder.walletAddress) ++
        Numeric.hexStringToByteArray(rawOrder.authAddr) ++
        Numeric.toBytesPadded(rawOrder.amountS.asBigInteger, 32) ++
        Numeric.toBytesPadded(rawOrder.amountB.asBigInteger, 32) ++
        Numeric.toBytesPadded(BigInt(rawOrder.validSince).bigInteger, 32) ++
        Numeric.toBytesPadded(BigInt(rawOrder.validUntil).bigInteger, 32) ++
        Numeric.toBytesPadded(rawOrder.lrcFee.asBigInteger, 32) ++
        Array[Byte](buyNoMoreThanB.toByte, rawOrder.marginSplitPercentage.toByte)

      Numeric.toHexString(Hash.sha3(data))
    }
  }
}
