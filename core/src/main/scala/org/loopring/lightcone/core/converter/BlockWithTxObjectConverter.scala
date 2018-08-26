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

class BlockWithTxObjectConverter()(
  implicit
  val transactionConverter: TransactionConverter)
  extends Converter[ethj.BlockWithTxObject, BlockWithTxObject] {

  def convert(org: ethj.BlockWithTxObject): BlockWithTxObject = {
    var block = BlockWithTxObject()
      .withParentHash(Hash(org.parentHash))
      .withSha3Uncles(org.sha3Uncles)
      .withLogsBloom(org.logsBloom)
      .withTransactionsRoot(org.transactionsRoot)
      .withStateRoot(org.stateRoot)
      .withReceiptRoot(org.receiptRoot)
      .withMiner(Address(org.miner))
      .withDifficulty(Big(org.difficulty))
      .withTotalDifficulty(Big(org.totalDifficulty))
      .withExtraData(org.extraData)
      .withSize(Big(org.size))
      .withGasLimit(Big(org.gasLimit))
      .withGasUsed(Big(org.gasUsed))
      .withTimestamp(Big(org.timestamp))
      .withUncles(org.uncles.map(Hash(_)))
      .withTransactions(org.transactions.map(transactionConverter.convert))

    // fields will be null while block pending
    if (!org.hash.isEmpty) block = block.withHash(Hash(org.hash))
    if (!org.number.isEmpty) block = block.withNumber(Big(org.number))
    if (!org.nonce.isEmpty) block = block.withNonce(Big(org.nonce))

    block
  }
}
