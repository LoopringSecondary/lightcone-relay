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

package org.loopring.lightcone.core.etypes

import java.math.BigInteger

case class Big(bytes: Array[Byte]) {

  val number: BigInteger = {
    if (bytes.toString.startsWith("0x")) {
      BigInt.apply(bytes.toString.substring(2)).bigInteger
    } else {
      BigInt.apply(bytes).bigInteger
    }
  }

  override def toString: String = number.toString()

  // 0为等于，1为大于，-1为小于
  def cmp(n: Big): Int = number.compareTo(n.number)
  def add(n: Big): Big = Big(number.add(n.number).toByteArray)
  def sub(n: Big): Big = Big(number.subtract(n.number).toByteArray)
  def mul(n: Big): Big = Big(number.multiply(n.number).toByteArray)
  def div(n: Big): BigDecimal = {
    val bigDecX = BigDecimal.apply(number).bigDecimal
    val bigDecY = BigDecimal.apply(n.number).bigDecimal
    bigDecX.divide(bigDecY)
  }
}
