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
import org.loopring.lightcone.core.order.{ OrderErrorConst, OrderWriteHelper, ValidateResult }

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
    case _: OrderWriterSettings ⇒
    case req: SubmitOrderReq ⇒ req match {
      case _ if req.order.isEmpty ⇒ sender ! OrderErrorConst.ORDER_IS_EMPTY
      case _ ⇒
        val order = req.order.get
        val validateRst = helper.validateOrder(order)
        if (!helper.validateOrder(req.order.get).pass)
          sender ! ErrorResp(OrderErrorConst.FILL_PRICE_FAILED.errorCode, validateRst.rejectReason)
        else {
          val generatedOrder = helper.fillInOrder(order)

          Routers.orderAccessor ? SaveOrders(Seq(generatedOrder)) onComplete {
            case Success(seq: Seq[Any]) if seq.nonEmpty ⇒
              seq.head match {
                case OrderSaveResult.SUBMIT_SUCC ⇒
                  Routers.orderManager ! OrdersSaved(Seq(generatedOrder))
                  sender ! SubmitOrderResp(generatedOrder.getRawOrder.getEssential.hash)
                case OrderSaveResult.SUBMIT_FAILED ⇒
                  sender ! OrderErrorConst.SAVE_ORDER_FAILED
                case OrderSaveResult.ORDER_EXIST ⇒
                  sender ! OrderErrorConst.ORDER_EXIST
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
    }

    case req: CancelOrdersReq ⇒
      helper.validateSoftCancelSign(req.sign) match {
        case ValidateResult(false, reason) ⇒ sender ! ErrorResp(OrderErrorConst.SOFT_CANCEL_SIGN_CHECK_FAILED.errorCode, reason)
        case ValidateResult(true, _) ⇒
          Routers.orderAccessor ? SoftCancelOrders(req.cancelOption) onComplete {
            case Success(rst: FatOrdersSoftCancelled) ⇒
              if (rst.orders.isEmpty) {
                sender ! OrderErrorConst.NO_ORDER_WILL_BE_SOFT_CANCELLED
              } else {
                sender ! OrdersSoftCancelled(rst.orders.map(_.getRawOrder.getEssential.hash))
                Routers.orderManager ! OrdersSoftCancelled(rst.orders.map(_.getRawOrder.getEssential.hash))
                // notify socktio ! OrdersSoftCancelled(os.map(_.rawOrder.get.hash))
              }
            case Success(_) ⇒ sender ! OrderErrorConst.SOFT_CANCEL_FAILED
            case Failure(_) ⇒ sender ! OrderErrorConst.SOFT_CANCEL_FAILED

          }
      }
  }
}
