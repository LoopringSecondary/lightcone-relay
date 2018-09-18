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

package org.loopring.lightcone.core.block

import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack
import org.loopring.lightcone.proto.eth_jsonrpc.BlockWithTxHash

import scala.concurrent.Future

case class Block(
    blockHash: String,
    parentHash: String,
    blockNumber: String,
    blockTime: String
)

trait BlockAccessHelper {

  // 轮询分叉事件
  def repeatedJobToGetForkEvent(block: BlockWithTxHash): Future[ChainRolledBack]

  // 获取当前正在解析的块高度:
  // extractor需要遍历链上所有块，不允许漏块
  // 数据库记录为空时使用config中数据,之后一直使用数据库记录
  // 同一个块里的数据可能没有处理完 业务数据使用事件推送后全量更新方式不会有问题,其他地方需要做去重
  def getCurrentBlockNumber: Future[BigInt]

  // 获取当前将要解析的块数据
  def getCurrentBlock: Future[BlockWithTxHash]

  // 获取父块，先从数据库查询，如果不存在，根据parent hash在链上递归查询
  def getParentBlock(block: BlockWithTxHash): Future[BlockWithTxHash]
}
