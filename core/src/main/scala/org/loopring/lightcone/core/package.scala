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

  implicit class RichBig(big: Big) {
    def fromString(str: String) = big.withContent(str)

    def toBigInt = BigInt(getHexString, 16)

    def getBitIntString = toBigInt.toString()

    def getIntValue = toBigInt.intValue()

    def getHexString = {
      if (big.content.startsWith("0x")) {
        big.content.substring(2)
      } else {
        big.content
      }
    }
  }

}

