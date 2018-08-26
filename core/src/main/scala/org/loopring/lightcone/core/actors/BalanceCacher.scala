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
import org.loopring.lightcone.proto.balance.{ BalanceInfo, GetBalanceInfo }
import org.loopring.lightcone.proto.cache.{ CacheBalanceInfo, CachedBalanceInfo }
import org.loopring.lightcone.proto.deployment._

object BalanceCacher
  extends base.Deployable[BalanceCacherSettings] {
  val name = "balance_cacher"
  val isSingleton = false

  def props = Props(classOf[BalanceCacher])

  def getCommon(s: BalanceCacherSettings) =
    base.CommonSettings("", s.roles, s.instances)
}

class BalanceCacher extends Actor {
  def receive: Receive = {
    case settings: BalanceCacherSettings =>

    case m: GetBalanceInfo =>
      sender() ! BalanceInfo()

    case m: CacheBalanceInfo =>
      sender() ! CachedBalanceInfo()
  }
}