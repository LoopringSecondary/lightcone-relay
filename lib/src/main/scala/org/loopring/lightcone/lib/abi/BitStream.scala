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

package org.loopring.lightcone.lib.abi

import org.loopring.lightcone.lib.etypes._

case class BitStream() {

  var data: String

  def getData: String = {
    if (this.data.length.equals(0)) {
      "0x0"
    } else {
      "0x" + this.data
    }
  }

  public addNumber(x: number, numBytes = 4, forceAppend = true) {
    // Check if we need to encode this number as negative
    if (x < 0) {
      const encoded = abi.rawEncode(["int256"], [x.toString(10)]);
      const hex = encoded.toString("hex").slice(-(numBytes * 2));
      return this.addHex(hex, forceAppend);
    } else {
      return this.addBigNumber(new BigNumber(x), numBytes, forceAppend);
    }
  }

  def addNumber(x: Int, numBytes: Int = 4, forceAppend: Boolean = true): Unit = {
    if (x < 0) {

    } else {
      this.addBigNumber(BigInt(x), numBytes, forceAppend)
    }
  }

  def addBigNumber(x: BigInt, numBytes: Int = 32, forceAppend: Boolean = true) : Unit = {
    val formattedData = this.padString(x.toHex, numBytes * 2)
    this.insert(formattedData, forceAppend)
  }

  def addAddress(x: String, numBytes: Int = 20, forceAppend: Boolean = true): Unit = {
    val formattedData = this.padString(x.slice(0, 2), numBytes * 2)
    this.insert(formattedData, forceAppend)
  }

  def addHex(x: String, forceAppend: Boolean = true): Unit = {
    if (x.startsWith("0x")) {
      this.insert(x.slice(0, 2), forceAppend)
    } else {
      this.insert(x, forceAppend)
    }
  }

  // offset 暂时没有任何用途,看后续合约如何变化
  private def insert(x: String, forceAppend: Boolean = true): Int = {
    this.data += x
    0
  }

  private def padString(s: String, targetLength: Int): String = {
    var x: String = s

    if (x.length > targetLength) {
      throw new Exception("0x" + x + " is too big to fit in the requested length (" + targetLength + ")")
    }
    while(x.length < targetLength) {
      x = "0" + x
    }
    x
  }
}
