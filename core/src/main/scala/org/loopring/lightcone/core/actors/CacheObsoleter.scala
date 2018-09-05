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

import akka.util.Timeout
import scala.concurrent.ExecutionContext
import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import org.loopring.lightcone.core.actors.base.RepeatedJobActor
import org.loopring.lightcone.proto.block_chain_event._
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.proto.deployment._

import scala.concurrent.Future

object CacheObsoleter
  extends base.Deployable[CacheObsoleterSettings] {
  val name = "cache_obsoleter"
  override val isSingleton = true

  def getCommon(s: CacheObsoleterSettings) =
    base.CommonSettings(None, s.roles, 1)
}

class CacheObsoleter()(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends RepeatedJobActor {

  import context.dispatcher
  val name = CacheObsoleter.name
  var deadtime = 0l
  var lastHeartBeatTime = 0l
  val mediator = DistributedPubSub(context.system).mediator

  override def receive: Receive = super.receive orElse {
    case settings: CacheObsoleterSettings =>
      deadtime = settings.deadTime
      initAndStartNextRound(settings.deadTime)

    case balanceChanged: AddressBalanceChanged =>
    //      mediator ! Publish(name, Purge.Balance(address = balanceChanged.owner))

    case cutoff: Cutoff =>
      val event = Purge.AllOrderForAddress()
        //        .withAddress(cutoff.owner)
        .withCutoff(cutoff.cutoff)
      mediator ! Publish(name, event)

    case cutoffPair: CutoffPair =>
      val event = Purge.AllOrderForAddress()
        //        .withAddress(cutoffPair.owner)
        .withCutoff(cutoffPair.cutoff)
        .withMarket(cutoffPair.market)
      mediator ! Publish(name, event)

    case forkEvent: ChainRolledBack =>
      val event = Purge.AllAfterBlock()
      //        .withBlockNumber(forkEvent.forkBlockNumber)
      mediator ! Publish(name, event)

    case orderCalceled: OrderCancelled =>
      val event = Purge.Order()
      //        .withOrderHash(orderCalceled.orderHash)
      mediator ! Publish(name, event)

    case ringMined: RingMined =>
      ringMined.fills foreach {
        filled =>
          val event = Purge.Order()
          //            .withOrderHash(filled.orderHash)
          mediator ! Publish(name, event)
      }

    case ht: HeartBeat =>
      lastHeartBeatTime = System.currentTimeMillis

  }

  override def handleRepeatedJob() = for {
    deadtime1 <- Future.successful(System.currentTimeMillis - lastHeartBeatTime)
  } yield {
    if (deadtime1 > deadtime)
      mediator ! Publish(name, Purge.All())
  }
}