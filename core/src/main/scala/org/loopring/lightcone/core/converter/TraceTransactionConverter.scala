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

package org.loopring.lightcone.core.converter

import org.loopring.lightcone.proto.eth_jsonrpc.{ TraceTransaction => rTransaction }
import org.loopring.lightcone.proto.eth.{ TraceTransaction => dTransaction, TraceCall => dCall, Hash, Big, Hex, Address }
import org.loopring.lightcone.core._

class TraceTransactionConverter()(implicit val traceCallConverter: TraceCallConverter) extends EthDataConverter[rTransaction, dTransaction] {

  def convertDown(org: rTransaction): dTransaction = {
    val tx = dCall()
      .withFrom(Address().fromString(org.from))
      .withTo(Address().fromString(org.to))
      .withInput(Hex().fromString(org.input))
      .withOutput(Hex().fromString(org.output))
      .withGasUsed(Big().fromString(org.gasUsed))
      .withValue(Big().fromString(org.value))
      .withType(org.`type`)

    dTransaction()
      .withTx(tx)
      .withCalls(org.calls.map(x => traceCallConverter.convertDown(x)))
  }
}
