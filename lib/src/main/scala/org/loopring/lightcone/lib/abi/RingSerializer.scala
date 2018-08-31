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

import org.loopring.lightcone.lib.collection.{ BigintArraySerializer, BytesArraySerializer }
import org.loopring.lightcone.lib.solidity.Abi
import org.loopring.lightcone.proto.solidity._

case class RingSerializer(abiFunction: Abi.Function, abiEvent: Abi.Event)(
  implicit
  val bytesArraySerializer: BytesArraySerializer,
  val bigintArraySerializer: BigintArraySerializer) extends AbiSerializer[SubmitRingFunction, RingMinedEvent] {

  def decode(txinput: Array[Byte]): SubmitRingFunction = {

    val list = abiFunction.decode(txinput)
    val addressList = bytesArraySerializer.objectToBytesArray(list.get(0))
    //val bigintList = bigintArraySerializer.hex2Bigint(list.get(1))
    SubmitRingFunction()
      .withAddressList(addressList)
    //.withBigintArgsList()
  }

  def decode(log: String, topics: Seq[String]): RingMinedEvent = {
    RingMinedEvent()
  }

  def encode(data: SubmitRingFunction): String = ???

}
