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

import com.google.inject.Inject
import com.typesafe.config.Config
import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.core.database.OrderDatabase
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.proto.block.Block
import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class BlockAccessHelperImpl @Inject() (
    val config: Config,
    val accessor: EthClient,
    val db: OrderDatabase
)
  extends BlockAccessHelper {

  var blockNumberIndex = BigInt(0)
  val query = db.blocks

  def repeatedJobToGetForkEvent(block: BlockWithTxHash): Future[ChainRolledBack] = for {
    _ ← setCurrentBlock(block)
    forkBlock ← getParentBlock(block)
    forkEvent ← Future(getRollBackEvent(block, forkBlock))
  } yield forkEvent

  def getCurrentBlock: Future[BlockWithTxHash] = for {
    blockNumber ← getCurrentBlockNumber
    blockNumberStr = safeBlockHex(blockNumber)
    req = GetBlockWithTxHashByNumberReq(blockNumberStr)
    res ← accessor.getBlockWithTxHashByNumber(req)
  } yield res.getResult

  def getCurrentBlockNumber: Future[BigInt] = for {
    currentBlockNumber ← if (blockNumberIndex.compare(BigInt(0)).equals(0)) {
      for {
        latestBlockInDb ← query.getLatestBlock
      } yield latestBlockInDb match {
        case Some(x) ⇒ BigInt(x.blockNumber)
        case _       ⇒ config.getString("extractor.start-block").asBigInt
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

  private def setCurrentBlock(block: BlockWithTxHash): Future[Long] = {
    if (!block.number.asBigInt.compare(blockNumberIndex).equals(0)) {
      blockNumberIndex = block.number.asBigInt
    } else {
      blockNumberIndex += 1
    }
    query.insert(Block(
      blockHash = block.hash,
      parentHash = block.parentHash,
      blockNumber = block.number.asBigInteger.longValue(),
      createdAt = block.timestamp.asBigInteger.longValue()
    ))
  }

  def getParentBlock(block: BlockWithTxHash): Future[BlockWithTxHash] = for {
    parentBlockInDb ← query.getBlock(block.parentHash)
    result ← parentBlockInDb match {
      case Some(x) ⇒ Future.successful(
        BlockWithTxHash(
          hash = x.blockHash,
          number = safeBlockHex(x.blockNumber),
          parentHash = x.parentHash
        )
      )
      case _ ⇒ for {
        req ← Future(GetBlockWithTxHashByHashReq(block.parentHash))
        blockOnChainOpt ← accessor.getBlockWithTxHashByHash(req)
        blockOnChain ← getParentBlock(blockOnChainOpt.getResult)
      } yield blockOnChain
    }
  } yield result

  private def getRollBackEvent(block: BlockWithTxHash, forkBlock: BlockWithTxHash) = {
    if (forkBlock.equals(BlockWithTxHash())) {
      ChainRolledBack().withFork(false)
    } else if (forkBlock.hash.equals(block.parentHash)) {
      ChainRolledBack().withFork(false)
    } else {
      ChainRolledBack(block.number, block.hash, forkBlock.number, forkBlock.hash, true)
    }
  }

  private def safeBlockHex(blockNumber: BigInt): String = {
    "0x" + blockNumber.intValue().toHexString
  }
}
