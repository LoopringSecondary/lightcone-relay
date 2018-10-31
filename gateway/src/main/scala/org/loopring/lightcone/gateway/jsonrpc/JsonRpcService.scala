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

package org.loopring.lightcone.gateway.jsonrpc

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }

import com.googlecode.jsonrpc4j.{ ProxyUtil, JsonRpcServer ⇒ GoogleRpcServer }
import org.loopring.lightcone.gateway.api.service.{ BalanceService, BalanceServiceImpl, OrderService, OrderServiceImpl }
import org.loopring.lightcone.gateway.socketio.{ EventRegister, EventRegistering }

import scala.concurrent.{ ExecutionContext, Future }

class JsonRpcService {

  implicit val context: ExecutionContext = ExecutionContext.global

  val multiService = ProxyUtil.createCompositeServiceProxy(
    this.getClass.getClassLoader,
    Array(new BalanceServiceImpl, new OrderServiceImpl),
    Array(classOf[BalanceService], classOf[OrderService]),
    true
  )

  //  这里要注册广播事件
  // event = 事件名称
  // interval = 时间间隔(秒)
  // replyTo = 客户端接收事件
  lazy val registering = EventRegistering()
    .registering("getBalance", 10, "balance")
    .registering(EventRegister("getBalance", 20, "balance"))

  val server = new GoogleRpcServer(multiService)

  def getAPIResult(req: String): Future[Option[String]] = {
    val output = new ByteArrayOutputStream()
    val input = new ByteArrayInputStream(req.getBytes())
    server.handleRequest(input, output)
    Future(Some(output.toString()))
  }
}
