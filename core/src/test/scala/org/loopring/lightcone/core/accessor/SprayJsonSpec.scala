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
import spray.json._
import DefaultJsonProtocol._

class SprayJsonSpec extends FlatSpec {

  //  info("execute cmd [sbt core/'testOnly *SprayJsonSpec'] to test single spec of json format any type")
  //
  //  "spray json" should "convert back and front" in {
  //    val protocol = MyJsonProtocol
  //    val params = Seq[Any]("0x41", DebugParam("5s", "callTracer"))
  //    val request = JsonRpcReq(1, "2.0", "eth_getBlockByNumber", params)
  //    val jsonobj = request.toJson
  //    val recover = jsonobj.convertTo[JsonRpcReq]
  //    info(jsonobj.toString())
  //    info(s"method: ${recover.method}, id:${recover.id}, rpc:${recover.jsonrpc}")
  //    recover.params.map(x => info(s"type:${x.getClass.getName}, value:${x}"))
  //  }
  //
  //  case class JsonRpcReq(id: Int, jsonrpc: String, method: String, params: Seq[Any])
  //  case class DebugParam(timeout: String, tracer: String)
  //  case class CallArg(from: String, to: String, gas: String, gasPrice: String, value: String, data: String)
  //
  //  object MyJsonProtocol extends DefaultJsonProtocol {
  //
  //    implicit val reqFormat = jsonFormat4(JsonRpcReq)
  //    implicit val callFormat = jsonFormat6(CallArg)
  //    implicit val debugFormat = jsonFormat2(DebugParam)
  //
  //    def write(x: Any) = x match {
  //      case n: Int => JsNumber(n)
  //      case s: String => JsString(s)
  //      case c: Char => JsString(c.toString)
  //      case b: Boolean => JsBoolean(b)
  //      case _ => JsString("null")
  //    }
  //
  //    def read(value: JsValue) = ???
  //  }
}
