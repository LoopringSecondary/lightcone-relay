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

package org.loopring.lightcone.core.accessor

import org.loopring.lightcone.proto.eth_jsonrpc.{ GetBlockWithTxHashByHashReq, GetBlockWithTxHashByNumberReq, GetBlockWithTxObjectByHashReq, GetBlockWithTxObjectByNumberReq }
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class GetBlockSpec extends FlatSpec {

  info("execute cmd [sbt core/'testOnly *GetBlockSpec'] to test eth get block by number and hash")

  "eth get block with hash by number" should "contain hash list" in {
    val req = GetBlockWithTxHashByNumberReq("0xa8a0")
    val respFuture = for {
      resp <- accessor.geth.getBlockWithTxHashByNumber(req)
      result = accessor.blockWithTxHashConverter.convert(resp.getResult)
    } yield result

    val block = Await.result(respFuture, accessor.timeout.duration)
    info(s"block is $block")
  }

  "eth get block with object by number" should "contain hash list" in {
    val req = GetBlockWithTxObjectByNumberReq("0xa8a0")
    val respFuture = for {
      resp <- accessor.geth.getBlockWithTxObjectByNumber(req)
      result = accessor.blockWithTxObjectConverter.convert(resp.getResult)
    } yield result

    val block = Await.result(respFuture, accessor.timeout.duration)
    info(s"block is $block")
  }

  "eth get block with hash by hash" should "contain hash list" in {
    val req = GetBlockWithTxHashByHashReq("0x36465444dbec326cf815973fc3064bce9c1f7ec22631d69462dea396cdadd730")
    val respFuture = for {
      resp <- accessor.geth.getBlockWithTxHashByHash(req)
      result = accessor.blockWithTxHashConverter.convert(resp.getResult)
    } yield result

    val block = Await.result(respFuture, accessor.timeout.duration)
    info(s"block is $block")
  }

  "eth get block with object by hash" should "contain hash list" in {
    val req = GetBlockWithTxObjectByHashReq("0x36465444dbec326cf815973fc3064bce9c1f7ec22631d69462dea396cdadd730")
    val respFuture = for {
      resp <- accessor.geth.getBlockWithTxObjectByHash(req)
      result = accessor.blockWithTxObjectConverter.convert(resp.getResult)
    } yield result

    val block = Await.result(respFuture, accessor.timeout.duration)
    info(s"block is $block")
  }
}
