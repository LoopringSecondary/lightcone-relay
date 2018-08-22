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

import org.scalatest.FlatSpec

class AnyTypeJsonSpec extends FlatSpec {

  info("execute cmd [sbt core/'testOnly *AnyTypeJsonSpec'] to test single spec of json format any type")

  "eth balance" should "use accessor" in {
    val params = Seq[Any]("0x41", true)
    val request = JsonRpcRequest("1", "2.0", "eth_getBlockByNumber", params)
    val jsonobj = accessor.geth.formatJsonRequest(request)
    val recover = accessor.geth.parseJsonRequest(jsonobj)
    info(jsonobj.toString())
    info(s"method: ${recover.method}, id:${recover.id}, rpc:${recover.jsonrpc}")
    recover.params.map(x => info(s"type:${x.getClass.toString}, value:${x}"))
  }

}
