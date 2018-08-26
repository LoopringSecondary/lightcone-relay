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
import akka.pattern.ask
import akka.util.Timeout
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.proto.balance.{ BalanceInfo, GetBalanceInfo }
import org.loopring.lightcone.proto.cache.CacheBalanceInfo
import org.loopring.lightcone.proto.common.ErrorResp
import org.loopring.lightcone.proto.deployment._

import scala.concurrent.Future

object BalanceManager
  extends base.Deployable[BalanceManagerSettings] {
  val name = "balance_manager"
  val isSingleton = false

  def props = Props(classOf[BalanceManager])
  def getCommon(s: BalanceManagerSettings) =
    base.CommonSettings(s.id, s.roles, s.instances)
}

class BalanceManager()(implicit timeout: Timeout) extends Actor {
  import context.dispatcher
  var id = ""

  def receive: Receive = {
    case settings: BalanceManagerSettings =>
      id = settings.id

    case getBalanceInfo: GetBalanceInfo => for {
      res <- Routers.balanceCacher ? getBalanceInfo
      info <- res match {
        case err: ErrorResp => for {
          ethRes <- Routers.ethereumAccessor ? getBalanceInfo
        } yield {
          ethRes match {
            case err: ErrorResp => err
            case info: BalanceInfo =>
              Routers.balanceCacher ! CacheBalanceInfo()
              info
          }
        }

        case info: BalanceInfo =>
          Future.successful(info)
      }
    } yield info
  }
}