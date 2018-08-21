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
import akka.cluster._
import akka.routing._
import akka.cluster.routing._

import scala.concurrent.duration._
import org.loopring.lightcone.core.routing.Routers
import com.typesafe.config.Config
import org.loopring.lightcone.data.deployment._
import org.loopring.lightcone.proto.order.{ GetOrder, OneOrder }
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import org.loopring.lightcone.proto.order.GetOrderResp
import org.loopring.lightcone.proto.order.GetOrdersResp

import scala.concurrent.Await

object OrderReader
  extends base.Deployable[OrderReaderSettings] {
  val name = "order_reader"
  val isSingleton = false

  def props = Props(classOf[OrderReader])

  def getCommon(s: OrderReaderSettings) =
    base.CommonSettings("", s.roles, s.instances)

}

class OrderReader() extends Actor {
  implicit val timeout = Timeout(2 seconds)

  def receive: Receive = {
    case settings: OrderReaderSettings =>
    case req: GetOrder => {
      val oneOrderResult = Routers.orderManager ? req
      val st = oneOrderResult.mapTo[OneOrder]
      val oneOrder = Await.result(st, timeout.duration)
      mapToResp(oneOrder)
    }
    case _ =>
  }

  def mapToResp(oneOrder: OneOrder) = {
    GetOrderResp()
  }
}

