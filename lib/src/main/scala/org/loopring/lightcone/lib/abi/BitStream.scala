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

case class BitStream(initbs: Array[Byte]) {

  var data = initbs

  def getData: Array[Byte] = {
    if (this.data.length.equals(0)) {
      "0x0".getBytes()
    } else {
      "0x".getBytes ++ this.data
    }
  }

//  public addNumber(x: number, numBytes = 4, forceAppend = true) {
//    // Check if we need to encode this number as negative
//    if (x < 0) {
//      const encoded = abi.rawEncode(["int256"], [x.toString(10)]);
//      const hex = encoded.toString("hex").slice(-(numBytes * 2));
//      return this.addHex(hex, forceAppend);
//    } else {
//      return this.addBigNumber(new BigNumber(x), numBytes, forceAppend);
//    }
//  }

  def addAddress(address: String): Array[Byte] = data ++ Numeric.toBytesPadded(Numeric.toBigInt(address), 20)

  def addUint256(num: BigInt): Array[Byte] = data ++ Numeric.toBytesPadded(num.bigInteger, 32)

  // todo: fuk 负数问题
  def addInt(num: BigInt): Array[Byte] = data ++ Numeric.toBytesPadded(num.bigInteger, 2)

  def addBoolean(b: Boolean): Array[Byte] = data :+ (if (b) 1 else 0).toByte

  def addRawBytes(otherBytes: Array[Byte]): Array[Byte] = data ++ otherBytes

  def addHex(hex: String): Array[Byte] = data ++ Numeric.hexStringToByteArray(hex)

  def extractUint8(offset: Int): Int = extractNumber(offset, 1).intValue()

  def extractUint16(offset: Int): Int = extractNumber(offset, 2).intValue()

  def extractUint32(offset: Int): Int = extractNumber(offset, 4).intValue()

  def extractUint256(offset: Int): BigInt = extractNumber(offset, 32)

  def extractAddress(offset: Int): Array[Byte] = "0x".getBytes ++ this.extractBytesX(offset, 20)

  def extractBytes1(offset: Int): Array[Byte] = this.extractBytesX(offset, 1)

  def extractBytes32(offset: Int): Array[Byte] = this.extractBytesX(offset, 32)

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
