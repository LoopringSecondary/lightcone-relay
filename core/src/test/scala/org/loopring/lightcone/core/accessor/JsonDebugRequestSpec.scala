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

class JsonDebugRequestSpec extends FlatSpec {

  info("execute cmd [sbt core/'testOnly *JsonDebugRequestSpec'] to test single spec of json format any type")

  "debug trace json request" should "contain list of call" in {
    //    val params = Seq[Any]("0x4eeb4d51d7190dcad0186ed88654297cbe573c69a0ad2e42147ed003589d0c49")
    //    val request = JsonDebugRequest("1", "debug_traceTransaction", params)
    //    val jsonobj = accessor.geth.formatJsonDebugRequest(request)
    //    val recover = accessor.geth.parseJsonDebugRequest(jsonobj)
    //    info(jsonobj.toString())
    //    info(s"method: ${recover.method}, id:${recover.id}")
    //    recover.params.map(x => info(s"type:${x.getClass.getName()}, value:${x}"))
  }

}
