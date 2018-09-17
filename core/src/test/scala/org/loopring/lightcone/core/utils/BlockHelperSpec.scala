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
  info("execute cmd [sbt core/'testOnly *BlockHelperSpec -- -z getForkEvent'] to debug getForkEvent")

  val helper = new BlockHelperImpl(config, geth)

  val block = BlockWithTxHash()
    .withHash("0x02427f544559dd4ee66f4f1ecfaa02b3f85edb02185019f4795a605cb043fc21")
    .withNumber("0x" + Hex.toHexString(BigInt(43163).toByteArray))
    .withParentHash("0x29c3afc8afe149e8098711cb2bfd65daee4e8bbc9dc940062e8abe729546eb95")

  "getCurrentBlockNumber" should "just be initialized yet" in {
    val resultFuture = for {
      resp ← helper.getCurrentBlockNumber
    } yield resp

    val blockNumber = Await.result(resultFuture, timeout.duration)

    info(s"block number is ${blockNumber.bigInteger.toString}")
  }

  "getCurrentBlock" should "be blockWithTxHash" in {
    val resultFuture = for {
      res ← helper.getCurrentBlock
    } yield res

    val block = Await.result(resultFuture, timeout.duration)

    info(s"block is ${block.toProtoString}")
  }

  "getForkBlock" should "get first block on chain" in {
    val resultFuture = for {
      res ← helper.getForkBlock(block)
    } yield res

    val forkBlock = Await.result(resultFuture, timeout.duration)

    if (forkBlock.hash.isEmpty) {
      info(s"forkblock is empty")
    } else {
      info(s"detected block:${block.number.asBigInteger.toString}-${block.hash} \r\n" +
        s"forkBlockNumber:${forkBlock.number.asBigInteger.toString}-${forkBlock.hash}")
    }
  }

  "getForkEvent" should "get an valid fork event while helper.getBlockByHashInDb return an block with hash=0x78567d18469f00eb0146e22db758cf169688bc6a0a0a9f0c584f8a43c62cdd29 and number=0xa899" in {
    val resultFuture = for {
      res ← helper.repeatedJobToGetForkEvent(block)
    } yield res

    val event = Await.result(resultFuture, timeout.duration)

    info(event.toProtoString)
    info(s"event detected blockNumber:${event.detectedBlockNumber.asBigInteger.toString}")
    info(s"event fork blockNumber:${event.forkBlockNumber.asBigInteger.toString}")
  }

}
