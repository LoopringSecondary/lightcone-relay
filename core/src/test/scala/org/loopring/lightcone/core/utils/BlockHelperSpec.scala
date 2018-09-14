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

import org.loopring.lightcone.core.ethaccessor._
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class BlockHelperSpec extends FlatSpec {
  info("execute cmd [sbt core/'testOnly *BlockHelperSpec'] to debug all shoulder")
  info("execute cmd [sbt core/'testOnly *BlockHelperSpec -- -z getCurrentBlockNumber'] to debug getCurrentBlockNumber")
  info("execute cmd [sbt core/'testOnly *BlockHelperSpec -- -z getCurrentBlock'] to debug getCurrentBlock")

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

}
