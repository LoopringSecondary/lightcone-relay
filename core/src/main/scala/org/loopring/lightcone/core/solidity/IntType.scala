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

package org.loopring.lightcone.core.solidity

import org.ethereum.util.ByteUtil
import java.math.BigInteger

object IntType {
  def encode(value: Any): Array[Byte] = {
    val bigInt = value match {
      case e: BigInt => e
      case e: BigInteger => BigInt(e)
      case e: Number => BigInt(e.toString)
      case e: Array[Byte] => BigInt(ByteUtil.bytesToBigInteger(e))
      case str: String =>
        val s = str.toLowerCase.trim
        if (s.startsWith("0x")) {
          BigInt(s.substring(2), 16)
        } else if (s.contains("a") || s.contains("b") || s.contains("c") ||
          s.contains("d") || s.contains("e") || s.contains("f")) {
          BigInt(s.substring(2), 16)
        } else {
          BigInt(s.substring(2), 10)
        }
      case _ =>
        throw new RuntimeException(s"Invalid value for type '$this': $value (${value.getClass})")
    }

    ByteUtil.bigIntegerToBytesSigned(bigInt.bigInteger, 32)
  }

  def decode(encoded: Array[Byte], offset: Int): BigInt = {
    BigInt(encoded.slice(offset, offset + 32))
  }
}

case class IntType(val name: String) extends SolidityType {
  def getCanonicalName() = {
    if (name.equals("int")) "int256"
    else if (name.equals("uint")) "uint256"
    else throw new RuntimeException(s"bad IntType name $name")
  }

  def encode(value: Any): Array[Byte] =
    IntType.encode(value)

  def decode(encoded: Array[Byte], offset: Int): Any =
    IntType.decode(encoded, offset)
}
