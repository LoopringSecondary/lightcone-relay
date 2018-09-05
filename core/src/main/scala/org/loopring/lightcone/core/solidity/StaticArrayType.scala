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
import collection.JavaConverters._

case class StaticArrayType(val name: String)
  extends ArrayType {
  val size = {
    val idx1 = name.indexOf("[")
    val idx2 = name.indexOf("]", idx1)
    name.substring(idx1 + 1, idx2).toInt
  }

  override def canonicalName() = {
    elementType.canonicalName() + "[" + size + "]"
  }

  def encodeSeq(seq: Seq[Any]): Array[Byte] = {
    if (seq.size != size) {
      throw new RuntimeException(
        s"List size ${seq.size} != ${size} for type $name")
    }
    seq
      .map(l => elementType.encode(l))
      .toArray.reduce(_ ++: _)
  }

  def decode(encoded: Array[Byte], offset: Int): Seq[Any] = {
    (0 to size).map { i =>
      elementType.decode(encoded, offset + i * elementType.fixedSize)
    }.toSeq
  }

  override val fixedSize = elementType.fixedSize * size
}
