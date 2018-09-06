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

import org.loopring.lightcone.proto.block_chain_event.{ Transfer, FullTransaction }

class AssembleTransferEventImpl extends Assembler[Transfer] {

  def convert(list: Seq[Any]): Transfer = {
    if (list.length != 3) {
      throw new Exception("length of transfer event invalid")
    }

    Transfer()
      .withSender(scalaAny2Hex(list(1)))
      .withReceiver(scalaAny2Hex(list(0)))
      .withValue(scalaAny2Hex(list(2)))
  }

  def txAddHeader(src: Transfer, tx: FullTransaction): Transfer = ???
  def txFullFilled(src: Transfer, tx: FullTransaction): Transfer = ???
}
