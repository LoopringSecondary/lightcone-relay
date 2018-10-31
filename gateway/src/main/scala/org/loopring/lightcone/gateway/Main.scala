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
import com.google.inject.Guice
import com.typesafe.config.ConfigFactory
import org.loopring.lightcone.gateway.api.service.BalanceService
import org.loopring.lightcone.gateway.socketio.{ SocketIOSettings, SocketIOSystemExtension }

object Main extends App {

  implicit val system = ActorSystem("Api", ConfigFactory.load())

  // 这里可以不依赖于 ActorSystem, 需要修改部分代码
  // 必须依赖于 Guice, 需要找到具体的实现类
  val injector = Guice.createInjector(CoreModule(system))

  // socketio server
  // register [trait or interface]
  // 这里只注册接口即可, 自动找到实现类
  val settings = SocketIOSettings().register[BalanceService]
  val server = SocketIOSystemExtension(system).init(injector, settings)
  server.start

}
