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
import akka.util.ByteString
import org.loopring.lightcone.proto.cache._
import org.loopring.lightcone.proto.common.ErrorResp
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.core.cache._
import redis._
import com.google.inject._

import scala.util.{ Failure, Success }

object OrderCacher
  extends base.Deployable[OrderCacherSettings] {
  val name = "order_cacher"

  def getCommon(s: OrderCacherSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class OrderCacher(
  settings: OrderCacherSettings,
  cache: OrderCache)(
  implicit
  ec: ExecutionContext,
  timeout: Timeout) extends Actor {

  // implicit val byteStringSerializer = new ByteStringSerializer[CachedMultiOrders] {
  //   def serialize(data: CachedMultiOrders): ByteString = {
  //     ByteString.fromArray(data.toByteArray)
  //   }
  // }

  // implicit val byteStringDeserializer = new ByteStringDeserializer[CachedMultiOrders] {
  //   def deserialize(data: ByteString): CachedMultiOrders = {
  //     CachedMultiOrders.parseFrom(data.toArray)
  //   }
  // }

  def receive: Receive = {
    case req: GetOrdersFromCache =>
    // redis.get(req.orderHashes.head) onComplete {
    //   case Success(_) =>
    //     sender ! CachedMultiOrders.defaultInstance
    //   case Failure(_) => ErrorResp()
    // }
    case m: Purge.Order =>

    case m: Purge.AllOrderForAddress =>

    case m: Purge.AllForAddresses =>

    case m: Purge.AllAfterBlock =>

    case m: Purge.All =>
  }
}