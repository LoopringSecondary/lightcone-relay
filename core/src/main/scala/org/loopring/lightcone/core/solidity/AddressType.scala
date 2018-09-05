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

import org.ethereum.util.ByteUtil

object AddressType extends IntType("address") {

  override def encode(value: Any): Array[Byte] = {
    val v = value match {
      case m: String if !m.startsWith("0x") => "0x" + m
      case m => m
    }
    val addr = IntType.encode(v)

    (0 to 12) foreach { i =>
      if (addr(i) != 0)
        throw new RuntimeException("Invalid address (should be 20 bytes length): "
          + ByteUtil.toHexString(addr))
    }
    addr
  }

  override def decode(encoded: Array[Byte], offset: Int) = {
    val bi = IntType.decode(encoded, offset).bigInteger
    ByteUtil.bigIntegerToBytes(bi, 20)
  }

}
