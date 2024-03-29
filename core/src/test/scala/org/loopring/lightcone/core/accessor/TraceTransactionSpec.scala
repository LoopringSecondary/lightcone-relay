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
import org.loopring.lightcone.proto.eth_jsonrpc.TraceTransactionReq
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class TraceTransactionSpec extends FlatSpec {
  info("execute cmd [sbt core/'testOnly *TraceTransactionSpec'] to test single spec of debug_traceTransaction")

  // curl localhost:8545 -X POST --header 'Content-type: application/json' --data '{"method":"debug_traceTransaction", "params":["0x4eeb4d51d7190dcad0186ed88654297cbe573c69a0ad2e42147ed003589d0c49", {"tracer":"callTracer", "timeout":"5s"}], "id":1}'

  "debug trace transaction" should "contain list of calls" in {
    val req = TraceTransactionReq("0x4eeb4d51d7190dcad0186ed88654297cbe573c69a0ad2e42147ed003589d0c49")
    val resultFuture = for {
      resp ← geth.traceTransaction(req)
    } yield resp.getResult

    val tx = Await.result(resultFuture, timeout.duration)

    info(tx.toString)
  }
}
