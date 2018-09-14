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

import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.core.ethaccessor._
import org.loopring.lightcone.proto.eth_jsonrpc.BlockWithTxHash
import org.scalatest.FlatSpec
import org.spongycastle.util.encoders.Hex

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class BlockHelperSpec extends FlatSpec {
  info("execute cmd [sbt core/'testOnly *BlockHelperSpec'] to debug all shoulder")
  info("execute cmd [sbt core/'testOnly *BlockHelperSpec -- -z getCurrentBlockNumber'] to debug getCurrentBlockNumber")
  info("execute cmd [sbt core/'testOnly *BlockHelperSpec -- -z getCurrentBlock'] to debug getCurrentBlock")
  info("execute cmd [sbt core/'testOnly *BlockHelperSpec -- -z getForkBlock'] to debug getForkBlock")

  val helper = new BlockHelperImpl(config, geth)

  "getCurrentBlockNumber" should "just be initialized yet" in {
    val resultFuture = for {
      resp <- helper.getCurrentBlockNumber
    } yield resp

    val blockNumber = Await.result(resultFuture, timeout.duration)

    info(s"block number is ${blockNumber.bigInteger.toString}")
  }

  "getCurrentBlock" should "be blockWithTxHash" in {
    val resultFuture = for {
      res <- helper.getCurrentBlock
    } yield res

    val block = Await.result(resultFuture, timeout.duration)

    info(s"block is ${block.toProtoString}")
  }

  "getForkBlock" should "get first block on chain" in {
    val blockNumber = "0x" + Hex.toHexString(BigInt(43163).toByteArray)
    val blockHash = "0x02427f544559dd4ee66f4f1ecfaa02b3f85edb02185019f4795a605cb043fc21"
    val block = BlockWithTxHash().withHash(blockHash).withNumber(blockNumber)

    val resultFuture = for {
      res <- helper.getForkBlock(block)
    } yield res

    val forkBlock = Await.result(resultFuture, timeout.duration)

    if (forkBlock.hash.isEmpty) {
      info(s"forkblock is empty")
    } else {
      info(s"detected block:${blockNumber.asBigInteger.toString}-$blockHash \r\n" +
        s"forkBlockNumber:${forkBlock.number.asBigInteger.toString}-${forkBlock.hash}")
    }
  }

}
