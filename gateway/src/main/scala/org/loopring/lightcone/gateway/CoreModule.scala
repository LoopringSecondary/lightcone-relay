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
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.google.inject.{ AbstractModule, Injector, Provides, Singleton }
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule
import org.loopring.lightcone.gateway.api.HttpAndIOServer
import org.loopring.lightcone.gateway.api.service.{ BalanceService, BalanceServiceImpl }
import org.loopring.lightcone.gateway.jsonrpc.{ JsonRpcServer, JsonRpcSettings }
import org.loopring.lightcone.gateway.socketio.{ EventRegistering, SocketIOServer }
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

object CoreModule {

  def apply(implicit system: ActorSystem): CoreModule = new CoreModule

}

class CoreModule(implicit system: ActorSystem)
  extends AbstractModule with ScalaModule {

  override def configure(): Unit = {

    bind[ActorSystem].toInstance(system)

    bind[Config].toInstance(system.settings.config)

    bind[ActorMaterializer].toInstance(ActorMaterializer())

    bind[BalanceService].to[BalanceServiceImpl]

    val databaseConfig = DatabaseConfig.forConfig[JdbcProfile]("slick-mysql", system.settings.config)
    val session: SlickSession = SlickSession.forConfig(databaseConfig)
    bind[SlickSession].toInstance(session)

    system.registerOnTermination(() ⇒ session.close())
  }

  //  @Provides
  //  @Singleton
  //  def providerHttpAndIOServer(
  //    implicit
  //    system: ActorSystem,
  //    mat: ActorMaterializer,
  //    injector: Injector
  //  ): HttpAndIOServer = {
  //
  //    // 这里注册需要反射类
  //    val settings = JsonRpcSettings().register[BalanceServiceImpl]
  //
  //    val jsonRpcServer = new JsonRpcServer(settings)
  //
  //    // 这里注册定时任务
  //    val registering = EventRegistering()
  //      .registering("getBalance", 10, "balance")
  //
  //    val ioServer = new SocketIOServer(jsonRpcServer, registering)
  //
  //    new HttpAndIOServer(jsonRpcServer, ioServer)
  //  }

}
