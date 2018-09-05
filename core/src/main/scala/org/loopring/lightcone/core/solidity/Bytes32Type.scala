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

import java.nio.charset.StandardCharsets

case class Bytes32Type(val name: String) extends SolidityType {
  def encode(value: Any): Array[Byte] = value match {
    case m: Number => IntType.encode(BigInt(value.toString))
    case m: String =>
      val res = new Array[Byte](32)
      val bytes = m.getBytes(StandardCharsets.UTF_8)
      bytes.copyToArray(res, 32 - bytes.length, bytes.length)
      res
    case m: Array[Byte] =>
      val res = new Array[Byte](32)
      m.copyToArray(res, 32 - m.length, m.length)
      res
  }

  def decode(encoded: Array[Byte], offset: Int): Any =
    encoded.slice(offset, offset + fixedSize)
}