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

import org.loopring.lightcone.proto.eth_jsonrpc.GetTransactionReceiptRequest
import org.loopring.lightcone.core._
import org.scalatest.FlatSpec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class GetTransactionReceiptSpec extends FlatSpec {
  info("execute cmd [sbt core/'testOnly *GetTransactionReceiptSpec'] to test single spec of eth_getTransactionReceipt")

  "receipt" should "contain logs" in {
    val req = GetTransactionReceiptRequest("0x3d07177d16e336c815802781ab3f5ca53b088726ec31be66bd19269b050413db")
    val resultFuture = for {
      resp <- accessor.geth.getTransactionReceipt(req)
      result = accessor.receiptconverter.convertDown(resp.getResult)
    } yield result

    val tx = Await.result(resultFuture, accessor.timeout.duration)

    info(tx.toString)
    //    info(s"blockhash:${tx.getBlockHash.Hex()}")
    //    info(s"transactionHash:${tx.getHash.Hex()}")
  }
}
