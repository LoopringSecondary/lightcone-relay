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

import org.loopring.lightcone.proto.eth_jsonrpc.{ BlockWithTxObject => rBlock }
import org.loopring.lightcone.proto.eth.{ BlockWithTxObject => dBlock, Hash, Big, Hex, Address }
import org.loopring.lightcone.core._

class BlockWithTxObjectConverter()(implicit val transactionConverter: TransactionConverter) extends EthDataConverter[rBlock, dBlock] {

  // todo(fukun): 拜占庭分叉前status默认为0
  def convertDown(org: rBlock): dBlock = {
    var block = dBlock()
      .withParentHash(Hash().fromString(org.parentHash))
      .withSha3Uncles(org.sha3Uncles)
      .withLogsBloom(org.logsBloom)
      .withTransactionsRoot(org.transactionsRoot)
      .withStateRoot(org.stateRoot)
      .withReceiptRoot(org.receiptRoot)
      .withMiner(Address().fromString(org.miner))
      .withDifficulty(Big().fromString(org.difficulty))
      .withTotalDifficulty(Big().fromString(org.totalDifficulty))
      .withExtraData(org.extraData)
      .withSize(Big().fromString(org.size))
      .withGasLimit(Big().fromString(org.gasLimit))
      .withGasUsed(Big().fromString(org.gasUsed))
      .withTimestamp(Big().fromString(org.timestamp))
      .withUncles(org.uncles.map(Hash().fromString(_)))
      .withTransactions(org.transactions.map(x => transactionConverter.convertDown(x)))

    // fields will be null while block pending
    if (!org.hash.isEmpty) block.withHash(Hash().fromString(org.hash))
    if (!org.number.isEmpty) block.withNumber(Big().fromString(org.number))
    if (!org.nonce.isEmpty) block.withNonce(Big().fromString(org.nonce))

    block
  }
}
