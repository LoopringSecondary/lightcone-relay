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

import org.loopring.lightcone.proto.{ eth_jsonrpc => ethj }
import org.loopring.lightcone.proto.eth._
import org.loopring.lightcone.core._

class TransactionReceiptConverter()(
  implicit
  val logConverter: ReceiptLogConverter)
  extends Converter[ethj.TransactionReceipt, TransactionReceipt] {

  // todo(fukun): 拜占庭分叉前status默认为0
  def convert(org: ethj.TransactionReceipt): TransactionReceipt = {
    var receipt = TransactionReceipt()
      .withBlockHash(Hash(org.blockHash))
      .withBlockNumber(Big(org.blockNumber))
      .withCumulativeGasUsed(Big(org.cumulativeGasUsed))
      .withFrom(Address(org.from))
      .withGasUsed(Big(org.gasUsed))
      .withLogsBloom(org.logsBloom)
      .withTo(Address(org.to))
      .withTransactionHash(Hash(org.transactionHash))
      .withTransactionIndex(Big(org.transactionIndex).getIntValue)
      .withLogs(org.logs.map(logConverter.convert))

    if (!org.contractAddress.isEmpty)

      receipt = receipt.withContractAddress(Address(org.contractAddress))
    if (!org.status.isEmpty)

      receipt = receipt.withStatus(Big(org.status).getIntValue)
    if (!org.root.isEmpty)
      receipt = receipt.withRoot(org.root)

    receipt
  }
}
