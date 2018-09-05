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

object BytesType extends SolidityType {
  val name = "bytes"
  override val isDynamicType = true

  def encode(value: Any): Array[Byte] = {
    val bb: Array[Byte] = value match {
      case m: Array[Byte] => m
      case m: String => m.getBytes
      case _ => throw new RuntimeException("byte[] or String value is expected for type 'bytes'")
    }
    val pad = new Array[Byte](((bb.length - 1) / 32 + 1) * 32)
    bb.copyToArray(pad, 0, bb.length)
    IntType.encode(bb.length) ++: pad
  }

  def decode(encoded: Array[Byte], offset: Int): Any = {
    val len = IntType.decode(encoded, offset).intValue()
    if (len == 0) new Array[Byte](0)
    val offset_ = offset + 32
    encoded.slice(offset_, offset_ + len)
  }
}
