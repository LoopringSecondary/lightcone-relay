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
import org.loopring.lightcone.core.order.{ OrderErrorConst, OrderWriteHelper }

import scala.concurrent.ExecutionContext
import org.loopring.lightcone.core.routing.Routers
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

class OrderWriter(helper: OrderWriteHelper)(implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor with ActorLogging {

  def receive: Receive = {
    case settings: OrderWriterSettings ⇒
    case req: SubmitOrderReq ⇒ {

      if (req.order.isEmpty) {
        OrderErrorConst.ORDER_IS_EMPTY
      }

      val order = req.order.get

      val validateRst = helper.validateOrder(order)
      if (!validateRst.pass) {
        ErrorResp(OrderErrorConst.FILL_PRICE_FAILED.errorCode, validateRst.rejectReason)
      }

      val generatedOrder = helper.fullInOrder(order)

      Routers.orderAccessor ? SaveOrders(Seq(generatedOrder)) onComplete {
        case Success(seq: Seq[Any]) if seq.nonEmpty ⇒
          seq.head match {
            case OrderSaveResult.SUBMIT_SUCC ⇒
              Routers.orderManager ! OrdersSaved(Seq(generatedOrder))
              sender ! SubmitOrderResp(generatedOrder.rawOrder.get.hash)
            case OrderSaveResult.SUBMIT_FAILED ⇒
              sender ! OrderErrorConst.SAVE_ORDER_FAILED
            case OrderSaveResult.Unrecognized(_) ⇒
              sender ! OrderErrorConst.SAVE_ORDER_FAILED
            case m ⇒
              log.error(s"unexpect SubmitOrderReq result: $m")
              sender ! OrderErrorConst.UNEXPECT_ORDER_SUBMIT_REQ
          }

        case Success(m) ⇒
          log.error(s"unexpect SubmitOrderReq result: $m")
          sender ! OrderErrorConst.UNEXPECT_ORDER_SUBMIT_REQ

        case Failure(e) ⇒
          log.error(s"unexpect SubmitOrderReq result: $e")
          sender ! OrderErrorConst.UNEXPECT_ORDER_SUBMIT_REQ
      }

    }
    case req: CancelOrdersReq ⇒
      if (!softCancelSignCheck(req.sign)) {
        new ErrorResp()

        Routers.orderAccessor ? SoftCancelOrders(req.cancelOption) onComplete {
          case Success(os) ⇒
            Routers.orderManager ! os
            os
          case Failure(_) ⇒ ErrorResp()
        }

      }
  }

  def softCancelSignCheck(sign: Option[SoftCancelSign]) = true
}

object OrderValidator {

}
