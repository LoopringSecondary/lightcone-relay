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

import java.math.BigInteger

import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.proto.ring.{ Ring, Rings }
import org.web3j.crypto.{ Hash ⇒ web3Hash, _ }
import org.web3j.utils.Numeric

package object richproto {

  val ethereumPrefix = "\u0019Ethereum Signed Message:\n"

  implicit class RichRawOrder(rawOrder: RawOrder) {

    def getSignerAddr(): String = {
      val orderHash = Numeric.hexStringToByteArray(rawOrder.getEssential.hash)
      val hash = web3Hash.sha3((ethereumPrefix + orderHash.length).getBytes() ++ orderHash)
      val (v, r, s) = convertSigToVRS(rawOrder.sig)
      val publicKey = Sign.recoverFromSignature(
        v,
        new ECDSASignature(r, s),
        hash
      )
      Keys.getAddress(publicKey)
    }

    def getHash(): String = {
      val essential = rawOrder.getEssential
      val data = Array[Byte]()
        .addUint256(essential.amountS.asBigInteger)
        .addUint256(essential.amountB.asBigInteger)
        .addUint256(essential.feeAmount.asBigInteger)
        .addUint256(BigInt(essential.validSince).bigInteger)
        .addUint256(BigInt(essential.validUntil).bigInteger)
        .addAddress(essential.owner)
        .addAddress(essential.tokenS)
        .addAddress(essential.tokenB)
        .addAddress(essential.dualAuthAddress)
        //todo:broker
        .addAddress(essential.broker)
        //todo:orderInterceptor
        .addAddress(essential.orderInterceptor)
        .addAddress(essential.wallet)
        .addAddress(essential.tokenRecipient)
        .addAddress(essential.feeToken)
        .addUint16(BigInt(essential.walletSplitPercentage).bigInteger)
        .addUint16(BigInt(essential.feePercentage).bigInteger)
        .addUint16(BigInt(essential.tokenSFeePercentage).bigInteger)
        .addUint16(BigInt(essential.tokenBFeePercentage).bigInteger)
        .addBoolean(essential.allOrNone)

      Numeric.toHexString(web3Hash.sha3(data))
    }
  }

  implicit class RichOrder(order: Order) {
    //todo:protocol 2.0 逻辑更改，需要重新计算
    def getRemained(): (Rational, Rational) = {
      val remainedAmountS = Rational(
        order.getRawOrder.getEssential.amountS.asBigInt - (
          order.dealtAmountS.asBigInt +
          order.cancelledAmountS.asBigInt +
          order.splitAmountS.asBigInt
        )
      )
      val remainedAmountB = Rational(
        order.getRawOrder.getEssential.amountB.asBigInt - (
          order.dealtAmountS.asBigInt +
          order.cancelledAmountB.asBigInt +
          order.splitAmountB.asBigInt
        )
      )
      (remainedAmountS, remainedAmountB)
    }

    def sellPrice(): Rational = {
      Rational(order.getRawOrder.getEssential.amountS.asBigInt, order.getRawOrder.getEssential.amountB.asBigInt)
    }

    def buyPrice(): Rational = {
      Rational(order.getRawOrder.getEssential.amountB.asBigInt, order.getRawOrder.getEssential.amountS.asBigInt)
    }

  }

  implicit class RichRings(rings: Rings) {

    def getHash(): String = {
      val data = rings.orders.foldLeft(Array[Byte]()) {
        (res, order) ⇒
          res.addHex(order.getEssential.hash)
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

  def convertSigToVRS(sig: String): (Int, BigInteger, BigInteger) = {
    val sigBytes = Numeric.hexStringToByteArray(sig)
    val v = if ((sigBytes(63) & 0xff) % 2 == 0) 1 else 0
    (v, sigBytes.slice(0, 32).asBigInteger(), sigBytes.slice(32, 64).asBigInteger())
  }

}
