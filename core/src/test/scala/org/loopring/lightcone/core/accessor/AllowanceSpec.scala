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

import org.loopring.lightcone.core.ethaccessor._
import org.loopring.lightcone.proto.eth_jsonrpc.GetAllowanceReq
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class AllowanceSpec extends FlatSpec {
  info("execute cmd [sbt core/'testOnly *AllowanceSpec'] to get allowance of erc20 token")

  "allowance" should "encode params and return amount of big number" in {

    val req = GetAllowanceReq()
      .withOwner(owner)
      .withToken(lrc)
      .withSpender(delegate)
      .withTag("latest")

    val resultFuture = for {
      resp <- geth.getAllowance(req)
    } yield resp

    val tx = Await.result(resultFuture, timeout.duration)

    info(tx.result.toString)
  }
}
