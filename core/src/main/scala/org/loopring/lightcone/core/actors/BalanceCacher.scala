/*
 * Copyright 2018 lightcore-relay
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
import akka.pattern.pipe
import akka.util.Timeout
import com.google.inject.Inject
import org.loopring.lightcone.core.cache._
import org.loopring.lightcone.proto.balance._
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.proto.deployment._

import scala.concurrent.ExecutionContext

object BalanceCacher
  extends base.Deployable[BalanceCacherSettings] {
  val name = "balance_cacher"

  def getCommon(s: BalanceCacherSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class BalanceCacher @Inject() (cache: BalanceCache)(implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor {

  var settings: BalanceCacherSettings = null

  DistributedPubSub(context.system).mediator ! Subscribe(CacheObsoleter.name, self)

  def receive: Receive = {
    case settings: BalanceCacherSettings ⇒
      this.settings = settings
    case m: GetBalancesReq ⇒
      cache.getBalances(m).pipeTo(sender)
    case m: GetAllowancesReq ⇒
      cache.getAllowances(m).pipeTo(sender)
    case m: GetBalanceAndAllowanceReq ⇒
      cache.getBalanceAndAllowances(m).pipeTo(sender)
    case m: CacheBalanceInfo ⇒
      cache.addCache(m).pipeTo(sender)
    case purgeEvent: Purge.Balance         ⇒

    case purgeEvent: Purge.AllForAddresses ⇒

    case purgeEvent: Purge.AllAfterBlock   ⇒

    case purgeEvent: Purge.All             ⇒
  }
}
