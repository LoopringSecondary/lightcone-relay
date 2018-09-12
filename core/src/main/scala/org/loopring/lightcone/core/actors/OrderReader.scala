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
import scala.concurrent.duration._
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.order._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import org.loopring.lightcone.proto.common.ErrorResp

import scala.concurrent._
import scala.util._

object OrderReader
  extends base.Deployable[OrderReaderSettings] {
  val name = "order_reader"

  def getCommon(s: OrderReaderSettings) =
    base.CommonSettings(None, s.roles, s.instances)

}

class OrderReader()(implicit
  ec: ExecutionContext,
  nodeContext: base.NodeContext,
  timeout: Timeout)
  extends Actor {

  def receive: Receive = {
    case settings: OrderReaderSettings =>

    case req: GetOrderReq =>
      val oneOrderResult = Routers.orderManager ? unwrapToQuery(req)
      oneOrderResult.mapTo[OneOrder] onComplete {
        case Success(o) => wrapToResp(o)
        case Failure(_) => ErrorResp()
      }

    case req: GetOrdersReq =>
      val multiOrderResult = Routers.orderManager ? unwrapToQuery(req)
      multiOrderResult.mapTo[MultiOrders] onComplete {
        case Success(o) => wrapToResp(o)
        case Failure(_) => ErrorResp()
      }
  }

  def wrapToResp(oneOrder: OneOrder) = {
    GetOrderResp()
  }

  def wrapToResp(oneOrder: MultiOrders) = {
    GetOrdersResp()
  }

  def unwrapToQuery(req: GetOrderReq) = {
    OrderQuery()
  }

  def unwrapToQuery(req: GetOrdersReq) = {
    OrderQuery()
  }
}

