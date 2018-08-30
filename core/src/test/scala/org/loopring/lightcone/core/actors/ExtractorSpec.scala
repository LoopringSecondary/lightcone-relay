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
//
//import akka.actor.{ActorSystem, Props}
//import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
//import org.loopring.lightcone.core.accessor.{EthClientImpl, GethClientConfig}
//import org.loopring.lightcone.lib.solidity.Abi
//import org.loopring.lightcone.proto.common.StartNewRound
//import org.loopring.lightcone.proto.token.Token
//import org.loopring.lightcone.proto.deployment.BlockchainEventExtractorSettings
//import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
//import org.spongycastle.util.encoders.Hex
//
//class ExtractorSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
//  with WordSpecLike with Matchers with BeforeAndAfterAll {
//
//  override def afterAll: Unit = {
//    TestKit.shutdownActorSystem(system)
//  }
//
//  val gethconfig = GethClientConfig.apply(host = "localhost", port = 8545, ssl = false)
//  val settings = BlockchainEventExtractorSettings(scheduleDelay = 5000) // 5s
//  val round = StartNewRound
//
//  //implicit val accessor = new EthClientImpl(gethconfig, abimap)
//  //val actor = system.actorOf(Props(new BlockchainEventExtractor()), "extractor")
//  // val probe = TestProbe()
//  // val testActor = TestActors
//  // val sender = system.actorOf(Props.empty)
//
//  "extractor actor" must {
//
//    "start single round" in {
//      //actor ! round
//      // sender() ! round
//      //Thread.sleep(300)
//    }
//  }
//}
