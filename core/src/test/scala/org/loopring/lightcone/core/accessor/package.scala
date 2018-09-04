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

package org.loopring.lightcone.core

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import akka.stream._
import scala.util._
import akka.http.scaladsl.model._
import akka.stream.scaladsl._
import akka.http.scaladsl._
import scala.concurrent._
import com.typesafe.config.ConfigFactory

package object accessor {

  type HttpFlow = Flow[ //
  (HttpRequest, Promise[HttpResponse]), //
  (Try[HttpResponse], Promise[HttpResponse]), //
  Http.HostConnectionPool]

  implicit val system = ActorSystem()

  val config = ConfigFactory.load()
  val contractABI = new ContractABI(config.getConfig("abi"))
  implicit val timeout = Timeout(5 seconds)

  val httpFlow = Http().cachedHostConnectionPool[Promise[HttpResponse]](
    host = "localhost",
    port = 8080)

  val ethClient: EthClient = new EthClientImpl(contractABI, httpFlow, 5)

  val owner = "0x1b978a1d302335a6f2ebe4b8823b5e17c3c84135"
  val lrc = "0xcd36128815ebe0b44d0374649bad2721b8751bef"
  val weth = "0xf079E0612E869197c5F4c7D0a95DF570B163232b"
  val delegate = "0xC533531f4f291F036513f7Abb41bfcCc62475486"
}
