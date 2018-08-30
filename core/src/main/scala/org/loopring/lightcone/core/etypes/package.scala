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

package object etypes {

  implicit class RichByteArray(bytes: Array[Byte]) {
    def asAddress(): Address = Address(bytes)

    def asHash(): Hash = Hash(bytes)

    def asBigInt(): BigInt = new String(bytes).asBigInt

    def asBigInteger(): BigInteger = new String(bytes).asBigInteger

  }

  implicit class RichString(hex: String) {

    def asAddress: Address = hex.getBytes.asAddress()

    def asHash: Hash = hex.getBytes.asHash()

    def asBigInt: BigInt = {
      if (!hex.toLowerCase.startsWith("0x")) BigInt(hex, 16)
      else BigInt(hex.substring(2), 16)
    }

    def asBigInteger: BigInteger = hex.asBigInt.bigInteger
  }

  implicit class RichBigInt(i: BigInt) {
    def toHex: String = i.toString(16)
  }

}
