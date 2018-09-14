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
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.loopring.lightcone.core.accessor.EthClientImpl
import org.loopring.lightcone.lib.abi.{ Erc20Abi, LoopringAbi }

import scala.concurrent.Promise
import scala.concurrent.duration._

package object ethaccessor {
  implicit val system = ActorSystem()

  val config = ConfigFactory.defaultApplication()
  val httpFlow = Http().cachedHostConnectionPool[Promise[HttpResponse]](
    host = config.getString("ethereum.host"),
    port = config.getInt("ethereum.port")
  )
  val erc20Abi = new Erc20Abi("abi/erc20.json")
  val loopringAbi = new LoopringAbi("abi/loopring.json")
  val queueSize = 5

  implicit val timeout = Timeout(5 seconds)
  val geth = new EthClientImpl(erc20Abi, loopringAbi, httpFlow, queueSize)

  val owner = "0x1b978a1d302335a6f2ebe4b8823b5e17c3c84135"
  val lrc = "0xcd36128815ebe0b44d0374649bad2721b8751bef"
  val weth = "0xf079E0612E869197c5F4c7D0a95DF570B163232b"
  val delegate = "0xC533531f4f291F036513f7Abb41bfcCc62475486"
}
