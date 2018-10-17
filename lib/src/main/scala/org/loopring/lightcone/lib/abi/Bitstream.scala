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

import org.web3j.utils.Numeric

case class Bitstream(str: String) {

  var data = Array.emptyByteArray

  def getData: String = {
    if (this.data.length.equals(0)) {
      "0x0"
    } else {
      Numeric.toHexString(data)
    }
  }

  /////////////////////////
  // functions for pack
  //
  /////////////////////////
  def addAddress(address: String, numBytes: Int = 20, forceAppend: Boolean = true) =
    insert(Numeric.toBytesPadded(Numeric.toBigInt(address), numBytes), forceAppend)

  // todo: fuk 负数问题
  def addNumber(num: BigInt, numBytes: Int = 4, forceAppend: Boolean = true) =
    insert(Numeric.toBytesPadded(num.bigInteger, numBytes), forceAppend)

  def addBigNumber(num: BigInt, numBytes: Int = 32, forceAppend: Boolean = true) =
    insert(Numeric.toBytesPadded(num.bigInteger, numBytes), forceAppend)

  def addBoolean(b: Boolean, forceAppend: Boolean = true) =
    insert(Array((if (b) 1 else 0).toByte), forceAppend)

  def addHex(hex: String, forceAppend: Boolean = true) =
    insert(Numeric.hexStringToByteArray(hex), forceAppend)

  def addRawBytes(str: String, forceAppend: Boolean = true) =
    insert(Numeric.hexStringToByteArray(str), forceAppend)

  private def insert(x: Array[Byte], forceAppend: Boolean): Int = {
    var offset = this.length

    if (!forceAppend) {
      // Check if the data we're inserting is already available in the bitstream.
      // If so, return the offset to the location.
      var start = 0
      var end = false

      while ((start != -1) && !end) {
        start = this.data.indexOf(x, start)
        if (start != -1) {
          if ((start % 2) == 0) {
            // logDebug("++ Reused " + x + " at location " + start / 2);
            offset = start / 2
            end = true
          } else {
            start += 1
          }
        }
      }
    }

    this.data ++= x

    val temp = Numeric.toHexString(this.data)
    offset
  }

  /////////////////////////
  // functions for unpack
  //
  /////////////////////////
  def extractUint8(offset: Int): Int = extractNumber(offset, 1).intValue()

  def extractUint16(offset: Int): Int = extractNumber(offset, 2).intValue()

  def extractUint32(offset: Int): Int = extractNumber(offset, 4).intValue()

  def extractUint256(offset: Int): BigInt = extractNumber(offset, 32)

  def extractAddress(offset: Int): String = Numeric.toHexString(this.extractBytesX(offset, 20))

  def extractBytes1(offset: Int): Array[Byte] = this.extractBytesX(offset, 1)

  def extractBytes32(offset: Int): Array[Byte] = this.extractBytesX(offset, 32)

  /////////////////////////
  // functions for properties
  //
  /////////////////////////
  def length(): Int = this.data.size / 2

  private def extractNumber(offset: Int, length: Int) = Numeric.toBigInt(this.extractBytesX(offset, length))

  private def extractBytesX(offset: Int, length: Int): Array[Byte] = {
    val start = offset * 2
    val end = start + length * 2
    if (this.data.length < end) {
      throw new Exception("substring index out of range:[\" + start + \", \" + end + \"]")
    }
    this.data.slice(start, end)
  }

}
