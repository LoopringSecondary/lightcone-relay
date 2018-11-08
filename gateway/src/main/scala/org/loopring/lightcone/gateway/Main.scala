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

package org.loopring.lightcone.gateway

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Guice
import com.typesafe.config.ConfigFactory
import net.codingwell.scalaguice.InjectorExtensions._
import org.loopring.lightcone.gateway.api.HttpAndIOServer
import org.loopring.lightcone.gateway.api.service.BalanceServiceImpl
import org.loopring.lightcone.gateway.jsonrpc.{ JsonRpcServer, JsonRpcSettings }
import org.loopring.lightcone.gateway.socketio.{ EventRegistering, SocketIOServer }

object Main extends App {

  implicit val system = ActorSystem("Api", ConfigFactory.load())

  implicit val injector = Guice.createInjector(CoreModule(system))

  implicit val mat = injector.instance[ActorMaterializer]

  // 这里注册需要反射类
  val settings = JsonRpcSettings().register[BalanceServiceImpl]

  val jsonRpcServer = new JsonRpcServer(settings)

  // 这里注册定时任务
  val registering = EventRegistering()
    .registering("getBalance", 10, "balance")

  val ioServer = new SocketIOServer(jsonRpcServer, registering)

  new HttpAndIOServer(jsonRpcServer, ioServer)
}
