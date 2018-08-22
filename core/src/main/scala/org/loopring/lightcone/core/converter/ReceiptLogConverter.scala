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

import org.loopring.lightcone.proto.eth_jsonrpc.{ Log => rLog }
import org.loopring.lightcone.proto.eth.{ Log => dLog, Hash, Big, Hex, Address }
import org.loopring.lightcone.core._

class ReceiptLogConverter extends EthDataConverter[rLog, dLog] {

  def convertDown(org: rLog) = dLog()
    .withLogIndex(Big().fromString(org.logIndex).Int)
    .withBlockNumber(Big().fromString(org.blockNumber))
    .withBlockHash(Hash().fromString(org.blockHash))
    .withTransactionHash(Hash().fromString(org.transactionHash))
    .withTransactionIndex(Big().fromString(org.transactionIndex).Int)
    .withAddress(Address().fromString(org.address))
    .withData(Hex().fromString(org.data))
    .withRemoved(org.removed)
    .withTopics(org.topics.map(Hex().fromString(_)))
}
