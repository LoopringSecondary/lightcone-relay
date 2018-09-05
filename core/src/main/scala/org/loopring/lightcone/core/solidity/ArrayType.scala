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

object ArrayType {
  def getType(typeName: String): ArrayType = {
    val idx1 = typeName.lastIndexOf("[")
    val idx2 = typeName.lastIndexOf("]")
    if (idx1 + 1 == idx2) {
      DynamicArrayType(typeName)
    } else {
      StaticArrayType(typeName)
    }
  }
}

trait ArrayType extends SolidityType {
  val name: String

  val elementType: SolidityType = {
    val idx = name.indexOf("[")
    val st = name.substring(0, idx)
    val idx2 = name.indexOf("]", idx)
    val subDim =
      if (idx2 + 1 == name.length) ""
      else name.substring(idx, idx2 + 1)

    SolidityType.getType(st + subDim)
  }

  def encode(value: Any): Array[Byte] = {
    value match {
      case array: Array[Any] => encodeSeq(array.toSeq)
      case seq: Seq[Any] => encodeSeq(seq)
      case _ => throw new RuntimeException(s"List value expected for type $name")
    }
  }

  def encodeSeq(seq: Seq[Any]): Array[Byte]

  // def getType(typeName: ArrayType): ArrayType = {
  //   val idx1 = typeName.lastIndexOf("[")
  //   val idx2 = typeName.lastIndexOf("]")
  //   if (idx1 + 1 == idx2) {
  //     new DynamicArrayType(typeName)
  //   } else {
  //     new StaticArrayType(typeName)
  //   }
  // }

}
