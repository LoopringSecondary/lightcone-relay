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

case class DynamicArrayType(val name: String)
  extends ArrayType {

  override val isDynamicType: Boolean = true

  override def canonicalName() = {
    elementType.canonicalName() + "[]"
  }

  def encodeSeq(seq: Seq[Any]): Array[Byte] = {
    var elems = Seq.empty[Array[Byte]]

    if (elementType.isDynamicType) {
      elems = elems :+ IntType.encode(seq.length)
      var offset = seq.length * 32

      (0 to seq.length) foreach { i =>
        elems = elems :+ IntType.encode(offset)
        val encoded = elementType.encode(seq(i))
      }
    } else {

    }
    // if (elementType.isDynamicType()) {
    //     elems = new byte[l.size() * 2 + 1][]
    //     elems[0] = IntType.encodeInt(l.size())
    //     int offset = l.size() * 32
    //     for (int i = 0 i < l.size() i++) {
    //         elems[i + 1] = IntType.encodeInt(offset)
    //         byte[] encoded = elementType.encode(l.get(i))
    //         elems[l.size() + i + 1] = encoded
    //         offset += 32 * ((encoded.length - 1) / 32 + 1)
    //     }
    // } else {
    //     elems = new byte[l.size() + 1][]
    //     elems[0] = IntType.encodeInt(l.size())

    //     for (int i = 0 i < l.size() i++) {
    //         elems[i + 1] = elementType.encode(l.get(i))
    //     }
    // }
    // return ByteUtil.merge(elems)
    null
  }

  def decode(encoded: Array[Byte], origOffset: Int): Seq[Any] = {

    val len = IntType.decode(encoded, origOffset).intValue()
    val origOffset_ = origOffset + 32
    var offset = origOffset_

    (0 to len) map { i =>
      val elem = if (elementType.isDynamicType) {
        elementType.decode(
          encoded, origOffset_ + IntType.decode(encoded, offset).intValue)
      } else {
        elementType.decode(encoded, offset)
      }
      offset += elementType.fixedSize
      elem
    }
  }

}
