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

import scala.concurrent.ExecutionContext
import akka.actor._
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.core.cache._

object OrderCacher
  extends base.Deployable[OrderCacherSettings] {
  val name = "order_cacher"

  def getCommon(s: OrderCacherSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class OrderCacher(cache: OrderCache)(
    implicit
    ec: ExecutionContext,
    timeout: Timeout
) extends Actor {

  def receive: Receive = {
    case settings: OrderCacherSettings ⇒
    case m: GetOrdersFromCache ⇒
      cache.getOrders(m).map(kv ⇒ CachedMultiOrders(kv))
    case m: SaveOrdersToCache ⇒
      cache.addOrUpdateOrders(m)
    case m: Purge.Order ⇒
      cache.purgeOrders(DelOrders(Seq(OrderReq(m.orderHash, m.owner))))
    case m: Purge.AllOrderForAddress ⇒
      cache.purgeAllForAddresses(Seq(m.address))
    case m: Purge.AllForAddresses ⇒
      cache.purgeAllForAddresses(m.addresses)
    case m: Purge.AllAfterBlock ⇒
      //TODO(小露) 这里需要和符坤商量下blockNumber数据类型
      cache.purgeAllAfterBlock(m.from.toLong, m.newest.toLong)
    case m: Purge.All ⇒
      cache.purgeAll()
  }
}
