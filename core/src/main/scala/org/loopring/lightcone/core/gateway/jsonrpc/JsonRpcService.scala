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

package org.loopring.lightcone.core.gateway.jsonrpc

import io.github.shogowada.scala.jsonrpc.serializers.UpickleJSONSerializer
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer
import org.loopring.lightcone.core.gateway.api.service.{ BalanceService, BalanceServiceImpl, OrderService, OrderServiceImpl }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class JsonRpcService {

  implicit val context: ExecutionContext = ExecutionContext.global

  val jsonSerializer = UpickleJSONSerializer()
  val jsonRpcServer = JSONRPCServer(jsonSerializer)
  jsonRpcServer.bindAPI[OrderService](new OrderServiceImpl)
  jsonRpcServer.bindAPI[BalanceService](new BalanceServiceImpl)
  println("1--------------> " + jsonRpcServer)
  println("2--------------> " + jsonRpcServer.requestJSONHandlerRepository)
  println("3--------------> " + jsonRpcServer.requestJSONHandlerRepository.get("getBalance"))

  def getAPIResult(req: String): Future[Option[String]] = {
    println("aaaaaaaaaaaaaaa")
    println(req)
    println(jsonRpcServer.requestJSONHandlerRepository.get("getBalance"))
    println("bbbbbbbbbbbbbb")
    val t = jsonRpcServer.receive(req)
    t onComplete {
      case Success(value)     ⇒ println("success" + value)
      case Failure(exception) ⇒ println("failed" + exception)
    }
    println("------------------> " + t)
    t
  }
}
