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
import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.proto.eth_jsonrpc.{ BlockWithTxHash, GetBlockWithTxHashByNumberReq }
import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class Block(
  blockHash: String,
  parentHash: String,
  blockNumber: String,
  blockTime: String)

trait ExtractorBlockDetector {
  def getBlock(): Future[BlockWithTxHash]
  def getCurrentBlock(): Future[BigInt]
  def setCurrentBlock(block: Block): Future[Unit]
  def getForkBlock(): Future[BigInt]
  def isBlockForked(): Future[Boolean]
  def getForkEvent(): Future[ChainRolledBack]
}

class ExtractorBlockDetectorImpl @Inject() (
  val config: Config,
  val accessor: EthClient) extends ExtractorBlockDetector {

  // cancel order: 43163
  // submit ring: 43206
  // cutoff all: 43168
  // cutoff pair: 43170
  val currentBlock = BigInt(43163)

  def getBlock(): Future[BlockWithTxHash] = for {
    _ <- Future {}
    block = safeBlockHex(currentBlock)
    blockReq = GetBlockWithTxHashByNumberReq(block)
    block <- accessor.getBlockWithTxHashByNumber(blockReq)
  } yield block.getResult

  // todo: get block from config and compare with data in db, set the big one as current block while server restart
  // todo: compare this.currentBlock and Block.blockNumber, set the bigger as current block while server running
  def getCurrentBlock(): Future[BigInt] = for {
    _ <- Future {}
  } yield currentBlock

  // todo: set this.currentBlock and write in db
  def setCurrentBlock(block: Block): Future[Unit] = for {
    _ <- Future {}
  } yield null

  // todo: find parent block in db, if not exist, get it from geth/parity recursive, and return
  def getForkBlock(): Future[BigInt] = for {
    ret <- Future { BigInt(0) }
  } yield ret

  def isBlockForked(): Future[Boolean] = for {
    _ <- Future {}
  } yield false

  // todo
  def getForkEvent(): Future[ChainRolledBack] = for {
    _ <- Future {}
  } yield ChainRolledBack()

  // todo: 使用Hex.toHexString会导致多出一些0,而现有的方式为转为int后toHexString
  private def safeBlockHex(blockNumber: BigInt): String = {
    "0x" + blockNumber.intValue().toHexString
  }
}
