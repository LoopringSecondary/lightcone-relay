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

import scala.concurrent.{ ExecutionContext, Future }
import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.cluster._
import akka.routing._
import akka.cluster.routing._
import org.loopring.lightcone.core.routing.Routers
import com.typesafe.config.Config
import org.loopring.lightcone.proto.balance.{ GetBalanceAndAllowanceReq, GetBalanceAndAllowanceResp }
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._

object OrderUpdater
  extends base.Deployable[OrderUpdaterSettings] {
  val name = "order_updater"

  def getCommon(s: OrderUpdaterSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class OrderUpdater()(implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor {
  val orderUpdateCoordinator = Routers.orderUpdateCoordinator
  val balanceManager = Routers.balanceManager
  val ethereumAccessor = Routers.ethereumAccessor

  def receive: Receive = {
    case settings: OrderUpdaterSettings ⇒
    case m: UpdateOrders ⇒ for {
      updatedOrders ← Future.sequence(m.orders.map { order ⇒
        for {
          getBalanceAndAllowanceReq ← Future.successful(GetBalanceAndAllowanceReq(
            address = order.rawOrder.get.owner,
            tokens = Seq(order.rawOrder.get.tokenS, "lrcAddress"),
            delegates = Seq(order.rawOrder.get.delegateAddress)
          ))
          balanceAndAllowance ← (balanceManager ? getBalanceAndAllowanceReq).mapTo[GetBalanceAndAllowanceResp]
          orderInfo ← ethereumAccessor ? GetOrderInfo()
        } yield {
          //todo:根据balance等确定order的状态以及交易量
          UpdatedOrder()
        }
      })
    } yield {
      sender() ! updatedOrders
      orderUpdateCoordinator ! updatedOrders
    }
    case m: CalculateOrdersStatus ⇒
  }
}
