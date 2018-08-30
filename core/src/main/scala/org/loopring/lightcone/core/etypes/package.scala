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

    def asBigInt(): BigInt = {
      val str = new String(bytes).toLowerCase
      if (!str.startsWith("0x")) BigInt(bytes)
      else BigInt(str.substring(2))
    }

    def asBigInteger(): BigInteger = {
      val str = new String(bytes).toLowerCase
      if (!str.startsWith("0x")) new BigInteger(bytes)
      else new BigInteger(str.substring(2))
    }
  }

  implicit class RichString(hex: String) {

    def asAddress: Address = hex.getBytes.asAddress()

    def asHash: Hash = hex.getBytes.asHash()

    def asBigInt: BigInt = hex.getBytes.asBigInt()

    def asBigInteger: BigInteger = hex.getBytes.asBigInteger()
  }

}
