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

class JsonRpcRequestSpec extends FlatSpec {

  info("execute cmd [sbt core/'testOnly *JsonRpcRequestSpec'] to test single spec of json format any type")

  "json rpc request" should "convert back and front" in {
    val params = Seq[Any]("0x41", DebugParams("5s", "callTracer"))
    val request = JsonRpcRequest(1, "2.0", "eth_getBlockByNumber", params)
    val jsonobj = accessor.geth.formatJsonRpcRequest(request)
    val recover = accessor.geth.parseJsonRpcRequest(jsonobj)
    info(jsonobj.toString())
    info(s"method: ${recover.method}, id:${recover.id}, rpc:${recover.jsonrpc}")
    recover.params.map(x => info(s"type:${x.getClass.getName}, value:${x}"))
  }

}
