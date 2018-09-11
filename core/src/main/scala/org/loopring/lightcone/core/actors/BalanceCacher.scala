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
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import com.google.inject.Inject
import org.loopring.lightcone.proto.balance._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.core.cache._

object BalanceCacher
  extends base.Deployable[BalanceCacherSettings] {
  val name = "balance_cacher"

  def getCommon(s: BalanceCacherSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class BalanceCacher @Inject() (cache: BalanceCache)(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends Actor {

  var settings: BalanceCacherSettings = null

  DistributedPubSub(context.system).mediator ! Subscribe(CacheObsoleter.name, self)

  def receive: Receive = {
    case settings: BalanceCacherSettings =>
      this.settings = settings

    case m: GetBalancesReq =>
      val sender1 = sender()
      for {
        resp <- cache.getBalances(m)
      } yield sender1 ! resp

    case m: GetAllowancesReq =>
      val sender1 = sender()
      for {
        resp <- cache.getAllowances(m)
      } yield sender1 ! resp

    case m: GetBalanceAndAllowanceReq =>
      val sender1 = sender()
      for {
        resp <- cache.getBalanceAndAllowances(m)
      } yield sender1 ! resp

    case m: CacheBalanceInfo =>
      val sender1 = sender()
      for {
        resp <- cache.addCache(m)
      } yield sender1 ! resp

    case purgeEvent: Purge.Balance =>

    case purgeEvent: Purge.AllForAddresses =>

    case purgeEvent: Purge.AllAfterBlock =>

    case purgeEvent: Purge.All =>
  }
}