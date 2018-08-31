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

import com.google.protobuf.ByteString
import org.loopring.lightcone.lib.solidity.Abi
import org.loopring.lightcone.proto.solidity._

case class RingSerializer()(
  implicit
  val abiFunction: Abi.Function,
  val abiEvent: Abi.Event) extends AbiSerializer[SubmitRingFunction, RingMinedEvent] {

  def decode(txinput: Array[Byte]): SubmitRingFunction = {
    val list = abiFunction.decode(txinput)

    SubmitRingFunction()
  }

  def decode(log: String, topics: Seq[String]): RingMinedEvent = {
    RingMinedEvent()
  }

  def encode(data: SubmitRingFunction): String = ???

  def objectToBytesArray(src: Any): Seq[BytesArray] = src match {

    case arr: Array[Object] => {
      arr.toSeq.map(subarr =>
        subarr match {
          case s: Array[Object] =>
            BytesArray().withBytesList(s.toSeq.map(obj => obj match {
              case bytes: Array[Byte] => ByteString.copyFrom(bytes)
              case _ => throw new Exception("to sub array[object] failed")
            }))
          case _ => throw new Exception("sub array type error")
        })
    }
    case _ => throw new Exception("input type error")
  }

  def hex2Bigint(hex: String): BigInt = {
    if (hex.startsWith("0x")) {
      val subhex = hex.substring(2)
      BigInt(subhex, 16)
    } else {
      BigInt(hex, 16)
    }
  }

}
