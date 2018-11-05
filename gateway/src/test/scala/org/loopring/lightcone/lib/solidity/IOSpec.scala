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

package org.loopring.lightcone.lib.solidity

import akka.actor.ActorSystem
import com.google.inject.Guice
import com.typesafe.config.ConfigFactory
import net.codingwell.scalaguice.InjectorExtensions._
import org.loopring.lightcone.gateway.CoreModule
import org.loopring.lightcone.gateway.jsonrpc.JsonRpcServer
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, WordSpec }

class SocketIOSpec extends WordSpec with BeforeAndAfterAll with MockFactory {

  override def beforeAll(): Unit = {

    implicit val system = ActorSystem("ApiSpec", ConfigFactory.load())

    val injector = Guice.createInjector(CoreModule(system))

    injector.instance[JsonRpcServer]

  }

}
