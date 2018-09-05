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

trait SolidityType {
  val name: String
  def canonicalName() = name

  def encode(value: Any): Array[Byte]
  def decode(encoded: Array[Byte], offset: Int): Any
  def decode(encoded: Array[Byte]): Any = decode(encoded, 0)

  def fixedSize: Int = 32
  val isDynamicType: Boolean = false

  override def toString() = name
}

object SolidityType {
  def getType(typeName: String): SolidityType = {
    if (typeName.contains("[")) ArrayType.getType(typeName)
    else if ("bool".equals(typeName)) BoolType
    else if (typeName.startsWith("int") || typeName.startsWith("uint")) IntType(typeName)
    else if ("address".equals(typeName)) AddressType
    else if ("string".equals(typeName)) StringType
    else if ("bytes".equals(typeName)) BytesType
    else if ("function".equals(typeName)) FunctionType
    else if (typeName.startsWith("bytes")) Bytes32Type(typeName)
    else throw new RuntimeException("Unknown type: " + typeName)
  }

}

