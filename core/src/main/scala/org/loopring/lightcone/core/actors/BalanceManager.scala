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
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.proto.balance._
import org.loopring.lightcone.proto.cache.CacheBalanceInfo
import org.loopring.lightcone.core.cache.actorcache._
import org.loopring.lightcone.proto.common.ErrorResp
import org.loopring.lightcone.proto.deployment._
import scalapb.GeneratedMessage
import akka.pattern.{ ask, pipe }
import scala.concurrent.Future

object BalanceManager
  extends base.Deployable[BalanceManagerSettings] {
  val name = "balance_manager"
  override val isSingleton = true //按照分片id，应当是singleton的

  def getCommon(s: BalanceManagerSettings) =
    base.CommonSettings(Some(s.id), s.roles, s.instances)
}

class BalanceManager()(
    implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor {

  val caching = new ActorCaching(
    cacheActor = Routers.balanceCacher,
    sourceActor = Routers.ethereumAccessor
  )

  var settings: BalanceManagerSettings = null
  def id = settings.id

  import BalanceManagerCachingFacilitators._
  def receive: Receive = {
    case settings: BalanceManagerSettings ⇒
      this.settings = settings

    case req: GetBalancesReq ⇒
      caching.askFor[GetBalancesReq, GetBalancesResp, CacheBalanceInfo](
        req
      ).pipeTo(sender)

    case req: GetAllowancesReq ⇒
      caching.askFor[GetAllowancesReq, GetAllowancesResp, CacheBalanceInfo](
        req
      ).pipeTo(sender)

    case req: GetBalanceAndAllowanceReq ⇒
      caching.askFor[GetBalanceAndAllowanceReq, GetBalanceAndAllowanceResp, CacheBalanceInfo](
        req
      ).pipeTo(sender)
  }
}
