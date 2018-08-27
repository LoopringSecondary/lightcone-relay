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

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import org.apache.commons.collections4.Predicate
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.lib.solidity.Abi
import org.spongycastle.util.encoders.Hex

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scalapb.json4s.JsonFormat
import spray.json._
import DefaultJsonProtocol._

object JsonRpcRequestFormat extends JsonFormat[JsonRpcRequest] {
  override def write(request: JsonRpcRequest): JsValue = JsObject(Map(
    "id" -> JsNumber(request.id),
    "jsonrpc" -> JsString(request.jsonrpc),
    "method" -> JsString(request.method),
    "params" -> JsArray(request.params.map(x => writeAny(x)): _*)))

  override def read(value: JsValue): JsonRpcRequest = {
    value.asJsObject.getFields("id", "jsonrpc", "method", "params") match {
      case Seq(JsNumber(id), JsString(jsonrpc), JsString(method), JsArray(params)) =>
        JsonRpcRequest(id.intValue(), jsonrpc, method, params)
      case _ => throw new Exception("JsonRpcRequest expected")
    }
  }

  private def writeAny(src: Any) = src match {
    case n: Int => JsNumber(n)
    case s: String => JsString(s)
    case b: Boolean if b.equals(true) => JsTrue
    case b: Boolean if b.equals(false) => JsFalse
    case o: DebugParams => JsObject(Map(
      "timeout" -> JsString(o.timeout),
      "tracer" -> JsString(o.tracer)))
    case o: CallArgs => {
      var map: Map[String, JsValue] = Map()
      if (!o.from.isEmpty) map += "from" -> JsString(o.from)
      if (!o.to.isEmpty) map += "to" -> JsString(o.to)
      if (!o.gas.isEmpty) map += "gas" -> JsString(o.gas)
      if (!o.gasPrice.isEmpty) map += "gasPrice" -> JsString(o.gasPrice)
      if (!o.value.isEmpty) map += "value" -> JsString(o.value)
      if (!o.data.isEmpty) map += "data" -> JsString(o.data)
      JsObject(map)
    }
    case _ => JsNull
  }

  private def readAny(value: JsValue) = value match {
    case JsNumber(n) => n.intValue()
    case JsString(s) => s
    case JsTrue => true
    case JsFalse => false
    case o: JsObject if o.fields.size.equals(2) => o.getFields("timeout", "tracer") match {
      case Seq(JsString(timeout), JsString(tracer)) => DebugParams(timeout, tracer)
      case _ => null
    }
    case o: JsObject if o.fields.size.equals(5) => o.getFields("from", "to", "gas", "gasPrice", "value", "data") match {
      case Seq(JsString(from), JsString(to), JsString(gas), JsString(gasPrice), JsString(v), JsString(data)) => CallArgs(from, to, gas, gasPrice, v, data)
      case _ => null
    }
    case _ => null
  }
}