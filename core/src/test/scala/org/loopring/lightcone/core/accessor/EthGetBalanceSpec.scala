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

import org.loopring.lightcone.proto.eth_jsonrpc.EthGetBalanceRequest
import org.loopring.lightcone.proto.eth.Big
import org.loopring.lightcone.core._
import org.scalatest.FlatSpec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class EthGetBalanceSpec extends FlatSpec {

  info("execute cmd [sbt core/'testOnly *EthGetBalanceSpec'] to test single spec of eth_getBalance")

  "eth balance" should "use accessor" in {
    val req = EthGetBalanceRequest()
      .withAddress("0x4bad3053d574cd54513babe21db3f09bea1d387d")
      .withTag("latest")
    val respFuture = for {
      resp <- accessor.geth.ethGetBalance(req)
      result = Big().fromString(resp.result)
    } yield result

    val amount = Await.result(respFuture, accessor.timeout.duration)
    info(s"geth eth_getBalance amount is ${amount.String}")
  }
}
