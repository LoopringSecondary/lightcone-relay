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

package org.loopring.lightcone.core.assemble

import org.loopring.lightcone.proto.block_chain_event.{ AddressBalanceChanged, FullTransaction }

class AssembleTransferEventImpl extends Assembler[AddressBalanceChanged] {

  def convert(list: Seq[Any]): Seq[AddressBalanceChanged] = {
    if (list.length != 3) {
      throw new Exception("length of transfer event invalid")
    }

    val sender = scalaAny2Hex(list(0))
    val receiver = scalaAny2Hex(list(1))
    val value = scalaAny2Bigint(list(2))

    val sendEvt = AddressBalanceChanged()
      .withOwner(sender)
      .withIncome(false)
      .withValue(value.toString)

    val recvEvt = AddressBalanceChanged()
      .withOwner(receiver)
      .withIncome(true)
      .withValue(value.toString)

    println(sendEvt.toProtoString)
    println(recvEvt.toProtoString)

    Seq(sendEvt, recvEvt)
  }

  def txAddHeader(src: AddressBalanceChanged, tx: FullTransaction): AddressBalanceChanged = ???
  def txFullFilled(src: AddressBalanceChanged, tx: FullTransaction): AddressBalanceChanged = ???
}
