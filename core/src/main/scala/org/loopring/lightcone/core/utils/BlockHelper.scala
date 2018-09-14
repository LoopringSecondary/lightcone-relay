/*
 * Copyright 2018 lightcore-relay
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

package org.loopring.lightcone.core.utils

import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack
import org.loopring.lightcone.proto.eth_jsonrpc.BlockWithTxHash

import scala.concurrent.Future

case class Block(
    blockHash: String,
    parentHash: String,
    blockNumber: String,
    blockTime: String
)

trait BlockHelper {
  def repeatedJobToGetForkEvent(block: BlockWithTxHash): Future[ChainRolledBack]
  def getCurrentBlockNumber: Future[BigInt]
  def getCurrentBlock: Future[BlockWithTxHash]
  def getForkBlock(block: BlockWithTxHash): Future[BlockWithTxHash]
}
