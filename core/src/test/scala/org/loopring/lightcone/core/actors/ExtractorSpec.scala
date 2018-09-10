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

package org.loopring.lightcone.core.actors

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.loopring.lightcone.core.accessor.EthClientImpl
import org.loopring.lightcone.core.utils._
import org.loopring.lightcone.lib.abi.{ Erc20Abi, LoopringAbi }
import org.loopring.lightcone.proto.common.StartNewRound
import org.loopring.lightcone.proto.deployment.BlockchainEventExtractorSettings
import org.loopring.lightcone.proto.token.{ Token, TokenList }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

import scala.concurrent.duration._
import scala.concurrent.Promise

class ExtractorSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  info("execute cmd [sbt core/'testOnly *ExtractorSpec'] test extractor worker")

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val config = ConfigFactory.defaultApplication()
  val httpFlow = Http().cachedHostConnectionPool[Promise[HttpResponse]](
    host = config.getString("ethereum.host"),
    port = config.getInt("ethereum.port"))
  val queueSize = 5

  val tokenlist = TokenList(list = Seq[Token](
    Token(
      protocol = "0xcd36128815ebe0b44d0374649bad2721b8751bef",
      symbol = "LRC",
      decimal = 18,
      source = "loopring",
      market = false),

    Token(
      protocol = "0xf079E0612E869197c5F4c7D0a95DF570B163232b",
      symbol = "WETH",
      decimal = 18,
      source = "ethereum",
      market = true)))

  val erc20Abi = new Erc20Abi(config)
  val loopringAbi = new LoopringAbi(config)
  val geth = new EthClientImpl(erc20Abi, loopringAbi, httpFlow, queueSize)

  implicit val detector = new ExtractorBlockDetectorImpl(config, geth)
  implicit val processor = new ExtractorTransactionProcessorImpl(tokenlist, geth, erc20Abi, loopringAbi)
  implicit val timeout = Timeout(200 second)

  val extractor = system.actorOf(Props(new BlockchainEventExtractor()), "extractor")

  "extractor actor" must {

    "start single round" in {
      val settings = BlockchainEventExtractorSettings(scheduleDelay = 5000) // 5s
      val round = StartNewRound()

      extractor ! round
      Thread.sleep(5000)
    }
  }
}
