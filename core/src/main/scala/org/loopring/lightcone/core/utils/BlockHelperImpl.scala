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
  val accessor: EthClient) extends BlockHelper {

  // cancel order: 43163
  // submit ring: 43206
  // cutoff all: 43168
  // cutoff pair: 43170
  var blockNumberIndex = BigInt(0)

  /**
    * 1.从链上获取当前块(块高度: 启动时根据配置文件及数据库记录初始化块高度)
    * 2.记录块数据，同时blockNumberIndex自增
    * 3.判断parenthash是否记录
    */
  def getForkEvent(block: BlockWithTxHash): Future[ChainRolledBack] = for {
    _ <- setCurrentBlock(block)
    forkBlock <- getParentBlockFromDb(block)
    forkEvent <- if (forkBlock.hash.equals(block.parentHash)) {
      ChainRolledBack().withFork(false)
    } else {
      ChainRolledBack(
        detectedBlockNumber = block.number,
        delectedBlockHash = block.hash,
        forkBlockNumber = forkBlock.number,
        forkBlockHash = forkBlock.hash,
        fork = true
      )
    }
  } yield forkEvent

  // 因为同一个块里的数据可能没有处理完 业务数据使用事件推送后全量更新方式 因为重复处理block不会有问题
  def getCurrentBlockNumber: Future[BigInt] = {
    if (blockNumberIndex.compare(BigInt(0)).equals(0)) {
      initBlockNumberIndex()
      Future(blockNumberIndex)
    } else {
      for {
        latestBlockOnChainRes <- accessor.ethGetBlockNumber()
        latestBlockOnChain = latestBlockOnChainRes.result.asBigInt
        _ = if (latestBlockOnChain.compare(blockNumberIndex) < 0) {
          blockNumberIndex = latestBlockOnChain
        }
      } yield blockNumberIndex
    }
  }

  private def setCurrentBlock(block: BlockWithTxHash): Future[Unit] = {
    if (!block.number.asBigInt.compare(blockNumberIndex).equals(0)) {
      blockNumberIndex = block.number.asBigInt
      writeBlockToDb(block)
    } else {
      blockNumberIndex += 1
      Future()
    }
  }

  // extractor需要遍历链上所有块，不允许漏块
  // 数据库记录为空时使用config中数据,之后一直使用数据库记录
  private def initBlockNumberIndex() = for {
    dbBlockNumber <- readLatestBlockFromDb
  } yield {
    blockNumberIndex = if (dbBlockNumber.compare(0).equals(0)) {
      config.getString("extractor.start-block").asBigInt
    } else {
      dbBlockNumber
    }
  }

  // 先从本地数据库寻找，本地存在则直接返回，不存在则从链上查询，递归调用
  def getParentBlockFromDb(block: BlockWithTxHash): Future[BlockWithTxHash] = for {
    parentBlockInDb <- getBlockByHashInDb(block.parentHash)
    result <- if (parentBlockInDb.hash.equals(block.parentHash)) {
      parentBlockInDb
    } else {
      for {
        req <- GetBlockWithTxHashByHashReq(block.parentHash)
        blockOnChain <- accessor.getBlockWithTxHashByHash(req)
      } yield getParentBlockFromDb(blockOnChain.getResult)
    }
  } yield result

  // todo: rely mysql
  private def getBlockByHashInDb(hash: String): Future[BlockWithTxHash] = for {
    _ <- Future()
  } yield BlockWithTxHash()

  // todo: rely mysql
  private def readLatestBlockFromDb: Future[BigInt] = for {
    block <- Future { BigInt(0) }
  } yield block

  // todo: rely mysql
  private def isParentHashExistInDb(hash: String): Future[Boolean] = for {
    exist <- Future { true }
  } yield exist

  // todo: rely mysql
  private def writeBlockToDb(block: BlockWithTxHash): Future[Unit] = for {
    _ <- Future()
  } yield null

  private def safeBlockHex(blockNumber: BigInt): String = {
    "0x" + blockNumber.intValue().toHexString
  }
}
