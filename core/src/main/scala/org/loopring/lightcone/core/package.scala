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

package org.loopring.lightcone

import org.loopring.lightcone.proto.eth._

package object core {

  implicit class RichHex(src: Hex) {
    def fromString(str: String) = src.withContent(str)
    def Hex() = src.content
  }

  implicit class RichAddress(src: Address) {
    def fromString(str: String) = src.withContent(str)
    def Hex() = src.content
  }

  implicit class RichHash(src: Hash) {
    def fromString(str: String) = src.withContent(str)
    def Hex() = src.content
  }

  implicit class RichBig(src: Big) {
    def fromString(str: String) = src.withContent(str)
    def String = BigNumber.toString()
    def BigNumber = BigInt(Hex, 16)
    def Int = BigNumber.intValue()

    def Hex = {
      if (src.content.startsWith("0x")) {
        src.content.substring(2)
      } else {
        src.content
      }
    }
  }

}

