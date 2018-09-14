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

package org.loopring.lightcone.core.utils

import com.google.inject.Inject
import com.typesafe.config.Config
import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.proto.eth_jsonrpc.{ BlockWithTxHash, GetBlockWithTxHashByHashReq, GetBlockWithTxHashByNumberReq }
import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class BlockHelperImpl @Inject() (
    val config: Config,
    val accessor: EthClient
)
  extends BlockHelper {

  var blockNumberIndex = BigInt(0)

  /** 1.从链上获取当前块(块高度: 启动时根据配置文件及数据库记录初始化块高度)
   *  2.记录块数据，同时blockNumberIndex自增
   *  3.判断parenthash是否记录
   */
  def repeatedJobToGetForkEvent(block: BlockWithTxHash): Future[ChainRolledBack] = for {
    _ ← setCurrentBlock(block)
    forkBlock ← getForkBlock(block)
    forkEvent ← Future(getRollBackEvent(block, forkBlock))
  } yield forkEvent

  def getCurrentBlock: Future[BlockWithTxHash] = for {
    blockNumber ← getCurrentBlockNumber
    blockNumberStr = safeBlockHex(blockNumber)
    req = GetBlockWithTxHashByNumberReq(blockNumberStr)
    res ← accessor.getBlockWithTxHashByNumber(req)
  } yield res.getResult

  // extractor需要遍历链上所有块，不允许漏块
  // 数据库记录为空时使用config中数据,之后一直使用数据库记录
  // 同一个块里的数据可能没有处理完 业务数据使用事件推送后全量更新方式不会有问题,其他地方需要做去重
  def getCurrentBlockNumber: Future[BigInt] = for {
    currentBlockNumber ← if (blockNumberIndex.compare(BigInt(0)).equals(0)) {
      for {
        dbBlockNumber ← readLatestBlockFromDb
      } yield if (dbBlockNumber.compare(0).equals(0)) {
        config.getString("extractor.start-block").asBigInt
      } else {
        dbBlockNumber
      }
    } else {
      for {
        blockOnChainRes ← accessor.ethGetBlockNumber()
        blockNumberOnChain = blockOnChainRes.result.asBigInt
      } yield if (blockNumberOnChain.compare(blockNumberIndex) < 0) {
        blockNumberOnChain
      } else {
        blockNumberIndex
      }
    }
  } yield currentBlockNumber

  // 先从本地数据库寻找，本地存在则直接返回，不存在则从链上查询，递归调用
  def getForkBlock(block: BlockWithTxHash): Future[BlockWithTxHash] = for {
    parentBlockInDb ← getBlockByHashInDb(block.parentHash)
    result ← if (parentBlockInDb.hash.equals(block.parentHash)) {
      Future(parentBlockInDb)
    } else if (parentBlockInDb.hash.isEmpty) {
      Future(BlockWithTxHash())
    } else {
      for {
        req ← Future(GetBlockWithTxHashByHashReq(block.parentHash))
        blockOnChainOpt ← accessor.getBlockWithTxHashByHash(req)
        blockOnChain ← getForkBlock(blockOnChainOpt.getResult)
      } yield blockOnChain
    }
  } yield result

  private def setCurrentBlock(block: BlockWithTxHash): Future[Unit] = {
    if (!block.number.asBigInt.compare(blockNumberIndex).equals(0)) {
      blockNumberIndex = block.number.asBigInt
    } else {
      blockNumberIndex += 1
    }
    saveBlock(block)
  }

  private def getRollBackEvent(block: BlockWithTxHash, forkBlock: BlockWithTxHash) = {
    if (forkBlock.equals(BlockWithTxHash())) {
      ChainRolledBack().withFork(false)
    } else if (forkBlock.hash.equals(block.parentHash)) {
      ChainRolledBack().withFork(false)
    } else {
      ChainRolledBack(block.number, block.hash, forkBlock.number, forkBlock.hash, true)
    }
  }

  // todo: relay mysql
  private def saveBlock(block: BlockWithTxHash): Future[Unit] = Future()

  // test fork: set hash, todo: rely mysql
  private def getBlockByHashInDb(hash: String): Future[BlockWithTxHash] =
    Future { BlockWithTxHash().withNumber("0xa899").withHash("0x78567d18469f00eb0146e22db758cf169688bc6a0a0a9f0c584f8a43c62cdd29") }

  // todo: rely mysql
  private def readLatestBlockFromDb: Future[BigInt] = for {
    block ← Future { BigInt(0) }
  } yield block

  private def safeBlockHex(blockNumber: BigInt): String = {
    "0x" + blockNumber.intValue().toHexString
  }
}
