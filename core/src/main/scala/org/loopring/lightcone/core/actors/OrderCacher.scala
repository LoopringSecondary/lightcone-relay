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
import akka.util.ByteString
import org.loopring.lightcone.proto.cache.{CachedMultiOrders, GetOrdersFromCache}
import org.loopring.lightcone.proto.common.ErrorResp
import org.loopring.lightcone.proto.deployment._
import redis.{ByteStringDeserializer, ByteStringSerializer, RedisClientPool}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object OrderCacher
  extends base.Deployable[OrderCacherSettings] {
  val name = "order_cacher"
  val isSingleton = false

  def props = Props(classOf[OrderCacher])

  def getCommon(s: OrderCacherSettings) =
    base.CommonSettings("", s.roles, s.instances)
}

class OrderCacher(implicit val redis: RedisClientPool) extends Actor {
  implicit val executor = ExecutionContext.global

  implicit val byteStringSerializer = new ByteStringSerializer[CachedMultiOrders] {
    def serialize(data: CachedMultiOrders): ByteString = {
      ByteString.fromArray(data.toByteArray)
    }
  }

  implicit val byteStringDeserializer = new ByteStringDeserializer[CachedMultiOrders] {
    def deserialize(data: ByteString): CachedMultiOrders = {
      CachedMultiOrders.parseFrom(data.toArray)
    }
  }

  def receive: Receive = {
    case settings: OrderCacherSettings =>
    case req: GetOrdersFromCache => redis.get(req.orderHashes.head) onComplete {
      case Success(_) =>
        sender ! CachedMultiOrders.defaultInstance
      case Failure(_) => ErrorResp()
    }
  }
}