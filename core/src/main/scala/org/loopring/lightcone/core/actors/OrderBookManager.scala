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
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.proto.deployment._

object OrderBookManager
  extends base.Deployable[OrderBookManagerSettings] {
  val name = "order_book_manager"
  val isSingleton = true

  def props = Props(classOf[OrderBookManager])

  def getCommon(s: OrderBookManagerSettings) =
    base.CommonSettings(s.id, s.roles, 1)
}

class OrderBookManager() extends Actor {
  DistributedPubSub(context.system).mediator ! Subscribe(CacheObsoleter.name, self)

  def receive: Receive = {
    case settings: OrderBookManagerSettings =>

    case purgeEvent: PurgeOrder =>

    case purgeEvent: PurgeAllOrderForAddress =>

    case purgeEvent: PurgeAllForAddresses =>

    case purgeEvent: PurgeAllAfterBlock =>

    case purgeEvent: PurgeAll =>

    case _ =>
  }
}