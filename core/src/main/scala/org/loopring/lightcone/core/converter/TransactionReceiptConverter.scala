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

import org.loopring.lightcone.proto.eth_jsonrpc.{ TransactionReceipt => rReceipt }
import org.loopring.lightcone.proto.eth.{ TransactionReceipt => dReceipt, Hash, Big, Hex, Address }
import org.loopring.lightcone.core._

class TransactionReceiptConverter()(implicit val logConverter: ReceiptLogConverter) extends EthDataConverter[rReceipt, dReceipt] {

  // todo(fukun): 拜占庭分叉前status默认为0
  def convertDown(org: rReceipt): dReceipt = {
    var receipt = dReceipt()
      .withBlockHash(Hash().fromString(org.blockHash))
      .withBlockNumber(Big().fromString(org.blockNumber))
      .withCumulativeGasUsed(Big().fromString(org.cumulativeGasUsed))
      .withFrom(Address().fromString(org.from))
      .withGasUsed(Big().fromString(org.gasUsed))
      .withLogsBloom(org.logsBloom)
      .withTo(Address().fromString(org.to))
      .withTransactionHash(Hash().fromString(org.transactionHash))
      .withTransactionIndex(Big().fromString(org.transactionIndex).Int)
      .withLogs(org.logs.map(x => logConverter.convertDown(x)))

    if (!org.contractAddress.isEmpty) receipt = receipt.withContractAddress(Address().fromString(org.contractAddress))
    if (!org.status.isEmpty) receipt = receipt.withStatus(Big().fromString(org.status).Int)
    if (!org.root.isEmpty) receipt = receipt.withRoot(org.root)

    receipt
  }
}
