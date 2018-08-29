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
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.proto.balance._
import org.loopring.lightcone.proto.deployment._

object BalanceReader
  extends base.Deployable[BalanceReaderSettings] {
  val name = "balance_reader"

  def getCommon(s: BalanceReaderSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class BalanceReader()(implicit timeout: Timeout) extends Actor {

  def receive: Receive = {
    case settings: BalanceReaderSettings =>
    case req: GetBalancesReq =>
      Routers.balanceManager forward req
    case req: GetAllowancesReq =>
      Routers.balanceManager forward req
    case req: GetBalanceAndAllowanceReq =>
      Routers.balanceManager forward req
    case _ =>
  }
}