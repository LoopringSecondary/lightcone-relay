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

import akka.actor._
import akka.cluster._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.routing._
import akka.cluster.routing._
import org.loopring.lightcone.core.routing.Routers
import com.typesafe.config.Config
import org.loopring.lightcone.core.actors.base.RepeatedJobActor
import org.loopring.lightcone.proto.block_chain_event.{AddressBalanceChanged, Cutoff, HeartBeat}
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.proto.deployment._

import scala.concurrent.Future

object CacheObsoleter
  extends base.Deployable[CacheObsoleterSettings] {
  val name = "cache_obsoleter"
  val isSingleton = true

  def props = Props(classOf[CacheObsoleter])

  def getCommon(s: CacheObsoleterSettings) =
    base.CommonSettings("", s.roles, 1)
}

class CacheObsoleter() extends RepeatedJobActor {
  import context.dispatcher
  var deadtime = 0l
  var lastHeartBeatTime = 0l
  val mediator = DistributedPubSub(context.system).mediator

  override def receive: Receive = super.receive orElse {
    case settings: CacheObsoleterSettings =>
      deadtime = settings.deadTime
      initAndStartNextRound(settings.deadTime)

    case ht: HeartBeat =>
      lastHeartBeatTime = System.currentTimeMillis

    case balanceChanged: AddressBalanceChanged =>
      mediator ! Publish("cache_obsoleter", PurgeBalance(owner = balanceChanged.owner))
    case cutoff: Cutoff =>
      mediator ! Publish("cache_obsoleter", PurgeAllOrderForAddress())
  }

  override def handleRepeatedJob() = for {
    deadtime1 <- Future.successful(System.currentTimeMillis - lastHeartBeatTime)
  } yield {
    if (deadtime1 > deadtime)
      mediator ! Publish("cache_obsoleter", PurgeAll())
  }
}