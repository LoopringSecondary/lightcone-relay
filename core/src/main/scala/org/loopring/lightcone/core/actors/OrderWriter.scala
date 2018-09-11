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
import scala.concurrent.ExecutionContext
import org.loopring.lightcone.core.routing.Routers
import scala.concurrent.duration._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.common.ErrorResp
import org.loopring.lightcone.proto.order._

import scala.util._

object OrderWriter
  extends base.Deployable[OrderWriterSettings] {
  val name = "order_writer"

  def getCommon(s: OrderWriterSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class OrderWriter(
  settings: OrderWriterSettings)(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends Actor {

  def receive: Receive = {
    case req: SubmitOrderReq => {
      if (req.rawOrder.isEmpty) {
        new ErrorResp()
      } else if (checkOrder(req.rawOrder.get)) {
        new ErrorResp()
      }

      Routers.orderAccessor ? unwrapToRawOrder(req) onComplete {
        case Success(os) =>
          Routers.orderManager ! os
          os
        case Failure(e) => ErrorResp()
      }

    }
    case req: CancelOrdersReq =>
      if (!softCancelSignCheck(req.sign)) {
        new ErrorResp()

        Routers.orderAccessor ? SoftCancelOrders(req.cancelOption) onComplete {
          case Success(os) =>
            Routers.orderManager ! os
            os
          case Failure(_) => ErrorResp()
        }

      }
  }

  def checkOrder(rawOrder: RawOrder) = true
  def unwrapToRawOrder(req: SubmitOrderReq) = RawOrder()
  def softCancelSignCheck(sign: Option[SoftCancelSign]) = true

}

object OrderValidator {

}
