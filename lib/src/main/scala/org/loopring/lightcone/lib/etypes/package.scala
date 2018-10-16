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

package org.loopring.lightcone.lib

import java.math.BigInteger

import com.google.protobuf.ByteString
import org.web3j.utils.Numeric

package object etypes {

  implicit class RichByteArray(bytes: Array[Byte]) {
    def asAddress(): Address = Address(bytes)

    def asHash(): Hash = Hash(bytes)

    def asBigInt(): BigInt = Numeric.toBigInt(bytes)

    def asBigInteger(): BigInteger = Numeric.toBigInt(bytes)

    def addAddress(address: String): Array[Byte] = bytes ++ Numeric.toBytesPadded(Numeric.toBigInt(address), 20)

    def addUint256(num: BigInteger): Array[Byte] = bytes ++ Numeric.toBytesPadded(num, 32)

    def addUint16(num: BigInteger): Array[Byte] = bytes ++ Numeric.toBytesPadded(num, 2)

    def addBoolean(b: Boolean): Array[Byte] = bytes :+ (if (b) 1 else 0).toByte

    def addRawBytes(otherBytes: Array[Byte]): Array[Byte] = bytes ++ otherBytes

    def addHex(hex: String): Array[Byte] = bytes ++ Numeric.hexStringToByteArray(hex)

  }

  implicit class RichString(hex: String) {

    def asAddress: Address = hex.getBytes.asAddress()

    def asHash: Hash = hex.getBytes.asHash()

    def asBigInt: BigInt = Numeric.toBigInt(hex)

    def asBigInteger: BigInteger = Numeric.toBigInt(hex)

    def asProtoByteString(): ByteString = {
      ByteString.copyFrom(hex.getBytes())
    }
  }

  implicit class RichBigint(i: BigInt) {

    def asProtoByteString(): ByteString = {
      ByteString.copyFrom(i.toByteArray)
    }

    def toHex: String = Numeric.toHexString(i.toByteArray)
  }

}
