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

class AnyTypeJsonSpec extends FlatSpec {

  info("execute cmd [sbt core/'testOnly *AnyTypeJsonSpec'] to test single spec of json format any type")

  "eth balance" should "use accessor" in {
    val params = Seq[Any]("0x41", true)
    val request = JsonRequest("1", "2.0", "eth_getBlockByNumber", params)
    val formater = JsonRequestFormat
    val result = request.toJson.toString()
    info(result)
  }

  case class JsonRequest(id: String, jsonrpc: String, method: String, params: Seq[Any]) {}

  implicit object JsonRequestFormat extends JsonFormat[JsonRequest] {
    override def write(request: JsonRequest): JsValue = JsArray(
      JsString(request.id),
      JsString(request.jsonrpc),
      JsString(request.method),
      JsArray(request.params.map(x => writeAny(x)): _*))

    private def writeAny(src: Any) = src match {
      case n: Int => JsNumber(n)
      case s: String => JsString(s)
      case b: Boolean if b.equals(true) => JsTrue
      case b: Boolean if b.equals(false) => JsFalse
      case _ => JsNull
    }

    override def read(value: JsValue): JsonRequest = {
      value.asJsObject.getFields("id", "jsonrpc", "method", "params") match {
        case Seq(JsString(id), JsString(jsonrpc), JsString(method), JsArray(params)) =>
          JsonRequest(id, jsonrpc, method, params)
        case _ => throw new Exception("Color expected")
      }
    }

    private def readAny(value: JsValue) = value match {
      case JsNumber(n) => n.intValue()
      case JsString(s) => s
      case JsTrue => true
      case JsFalse => false
      case _ => null
    }
  }

}
