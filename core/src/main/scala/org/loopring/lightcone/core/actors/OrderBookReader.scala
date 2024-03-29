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
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.orderbook.GetOrderBookReq

import scala.concurrent.ExecutionContext

object OrderBookReader
  extends base.Deployable[OrderBookReaderSettings] {
  val name = "order_book_reader"

  def getCommon(s: OrderBookReaderSettings) =
    base.CommonSettings(Option(s.id), s.roles, s.instances)
}

class OrderBookReader()(implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor {
  var settings: OrderBookReaderSettings = null

  def receive: Receive = {
    case settings: OrderBookReaderSettings ⇒
      this.settings = settings
    case m: GetOrderBookReq ⇒
      Routers.orderBookManager(settings.id) forward m
  }
}
