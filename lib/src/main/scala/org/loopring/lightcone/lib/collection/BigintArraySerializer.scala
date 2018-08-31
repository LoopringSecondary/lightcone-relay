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

package org.loopring.lightcone.lib.collection

trait BigintArraySerializer {
  def hex2Bigint(hex: String): BigInt
}

final class SimpleBigintArraySerializer extends BigintArraySerializer {

  def hex2Bigint(hex: String): BigInt = {
    if (hex.startsWith("0x")) {
      val subhex = hex.substring(2)
      BigInt(subhex, 16)
    } else {
      BigInt(hex, 16)
    }
  }
}