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

package org.loopring.lightcone.core.order

import com.google.inject.Inject
import org.loopring.lightcone.core.database.{OrderDatabase, tables}
import org.loopring.lightcone.proto.order.{Order, OrderChangeLog, OrderSaveResult}

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

class OrderAccessHelperImpl @Inject() (val module: OrderDatabase) extends OrderAccessHelper {

  implicit val profile = module.profile
  implicit val executor = module.dbec
  import profile.api._

  override def saveOrder(order: Order): Future[OrderSaveResult] = {

    if (order.rawOrder.isEmpty) {
      return Future(OrderSaveResult.SUBMIT_FAILED)
    }

    val getOrderRst = getOrderByHash(order.rawOrder.get.hash)
    val optOrder = Await.result(getOrderRst, 1 seconds)
    if (optOrder.isDefined) {
      return Future(OrderSaveResult.ORDER_EXIST)
    }

    val changeLog = OrderChangeLog()

    val a = (for {
      saveOrderRst ← tables.ordersQ += order
      saveLogRst ← tables.orderChangeLogsQ += changeLog
    } yield (saveOrderRst, saveLogRst)).transactionally

    val withErrorHandling = a.asTry.flatMap {
      case Failure(e: Throwable) ⇒ {
        // print log info
        // print(e)
        // DBIO.failed(e)
        DBIO.successful(OrderSaveResult.SUBMIT_FAILED)
      }
      case Success(_) ⇒ DBIO.successful(OrderSaveResult.SUBMIT_SUCC)
    }
    module.db.run(withErrorHandling)
  }

  override def getOrderByHash(orderHash : String) : Future[Option[Order]] = module.orders.getOrder(orderHash)
}
