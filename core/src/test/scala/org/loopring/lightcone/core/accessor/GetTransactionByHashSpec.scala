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

import org.loopring.lightcone.proto.eth_jsonrpc.GetTransactionByHashRequest
import org.scalatest.FlatSpec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class GetTransactionByHashSpec extends FlatSpec {
  info("execute cmd [sbt core/'testOnly *GetTransactionByHashSpec'] to test single spec of eth_getTransactionByHash")

  "transaction hash" should "contain gasLimit but without gasUsed" in {
    val req = GetTransactionByHashRequest().withHash("0x3d07177d16e336c815802781ab3f5ca53b088726ec31be66bd19269b050413db")
    val respFuture = for {
      resp <- accessor.geth.getTransactionByHash(req)
    } yield resp

    val result = Await.result(respFuture, accessor.timeout.duration)
    info(s"geth eth_getTransactionByHash hash:${result.getResult.blockHash}, from:${result.getResult.from}")
  }
}
