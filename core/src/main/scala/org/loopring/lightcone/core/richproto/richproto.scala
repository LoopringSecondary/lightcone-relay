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
import org.loopring.lightcone.proto.order.{Order, RawOrder}
import org.loopring.lightcone.proto.ring.Ring
import org.web3j.crypto.{Hash ⇒ web3Hash, _}
import org.web3j.utils.Numeric

package object richproto {

  val ethereumPrefix = "\u0019Ethereum Signed Message:\n"
  implicit class RichRawOrder(rawOrder: RawOrder) {

    def getSignerAddr(): String = {
      val orderHash = Numeric.hexStringToByteArray(rawOrder.hash)
      val hash = web3Hash.sha3((ethereumPrefix + orderHash.length).getBytes() ++ orderHash)
      val publicKey = Sign.recoverFromSignature(
        (rawOrder.v - 27).toByte,
        new ECDSASignature(rawOrder.r.asBigInteger, rawOrder.s.asBigInteger),
        hash
      )
      Keys.getAddress(publicKey)
    }

    def getHash(): String = {
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

      Numeric.toHexString(web3Hash.sha3(data))
    }
  }

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

    def sellPrice(): Rational = {
      Rational(order.rawOrder.get.amountS.asBigInt, order.rawOrder.get.amountB.asBigInt)
    }

    def buyPrice(): Rational = {
      Rational(order.rawOrder.get.amountB.asBigInt, order.rawOrder.get.amountS.asBigInt)
    }

  }

  implicit class RichRing(ring:Ring) {
    def getHash():String = {
      val data = ring.getUniqueId() ++
      Numeric.hexStringToByteArray(ring.feeReceipt) ++
        Numeric.toBytesPadded(ring.feeSelection(), 2)
      Numeric.toHexString(web3Hash.sha3(data))
    }

    def feeSelection(): BigInt = {
      var selection = BigInt(0)
      selection //todo:实现2.0 应该不需要该函数
    }

    def getUniqueId():Array[Byte] = {
      val uniqueId = ring.orders
        .map(order ⇒ BigInt(Numeric.toBigInt(order.rawOrder.get.hash)))
        .reduceLeft(_ ^ _)
      uniqueId.toByteArray
    }

  }
}
