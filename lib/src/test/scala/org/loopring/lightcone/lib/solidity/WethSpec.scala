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

package org.loopring.lightcone.lib.solidity

import org.loopring.lightcone.lib.abi.WethAbi
import org.loopring.lightcone.proto.block_chain_event.{ TxHeader, WethDeposit }
import org.loopring.lightcone.proto.eth_jsonrpc.Log
import org.scalatest.FlatSpec

class WethSpec extends FlatSpec {

  info("execute cmd [sbt lib/'testOnly *WethSpec'] to test single spec of submitRing")

  val abi = new WethAbi("abi/weth.json")

  "DepositEvent" should "contain owner and amount" in {
    val method = abi.findEventByName("Deposit")
    val data = "0x000000000000000000000000000000000000000000000000001550f7dca70000"
    val topics = Seq(
      "0xe1fffcc4923d04b559f4d29a8bfc6cda04eb5b0d3c460751c2402c5c5cc9109c",
      "0x0000000000000000000000006aa181a3b81d2f6c867a7afa8e4df130e38a7821"
    )
    val log = Log().withData(data).withTopics(topics)

    abi.decodeLogAndAssemble(log, TxHeader()).map(x ⇒ x match {
      case e: WethDeposit ⇒ info(e.toProtoString)
      case _              ⇒ info("unpack failed")
    })
  }

}
