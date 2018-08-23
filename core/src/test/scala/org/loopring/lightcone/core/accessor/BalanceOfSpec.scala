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

import org.apache.commons.collections4.Predicate
import org.loopring.lightcone.proto.eth_jsonrpc.BalanceOfRequest
import org.loopring.lightcone.core._
import org.loopring.lightcone.lib.solidity.Abi
import org.scalatest.FlatSpec
import org.spongycastle.util.encoders.Hex

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class BalanceOfSpec extends FlatSpec {
  info("execute cmd [sbt core/'testOnly *BalanceOfSpec'] to get balance of erc20 token")

  "balance" should "encode params and return amount of big number" in {
    val owner = "0x1b978a1d302335a6f2ebe4b8823b5e17c3c84135"
    val token = "0xcd36128815ebe0b44d0374649bad2721b8751bef" // lrc
    val req = BalanceOfRequest().withOwner(owner).withToken(token)

    val methodName: Predicate[Abi.Function] = (x) => x.name.equals("balanceOf")
    val method = accessor.geth.erc20Abi.findFunction(methodName)

    val data = method.encode(req.owner)

    //    val resultFuture = for {
    //      resp <- accessor.geth.balanceOf(req)
    //    } yield resp
    //
    //    val tx = Await.result(resultFuture, accessor.timeout.duration)

    info(Hex.toHexString(data))
    //info(s"blockhash:${tx.getBlockHash.Hex()}")
    //info(s"transactionHash:${tx.getHash.Hex()}")
  }
}
