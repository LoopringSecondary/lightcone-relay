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

package org.loopring.lightcone.core.cache

import akka.util.ByteString
import redis._
import redis.api.scripting.RedisScript
import com.google.inject._
import org.loopring.lightcone.core.cache.redishash.PurgeAllCache
import org.loopring.lightcone.proto.cache._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

final class OrderRedisCache @Inject() (
    val redis: RedisCluster
)(implicit val ec: ExecutionContext)
  extends OrderCache with PurgeAllCache {

  val ORDER_CACHE_PRE = "lt_orders_"
  val delimeter: String = "_"
  val UPDATED_BY_BLOCK_PRE = "change_at_block_"
  val cachePreKey: String = ORDER_CACHE_PRE

  private def scriptFromResource(resource: String) = {
    val file = getClass.getResource(resource).getFile
    val content = scala.io.Source.fromFile(file).mkString
    RedisScript(content)
  }

  private val PurgeOrdersAfterBlockScript = scriptFromResource("/purge_orders_by_fork_block.lua")
  private val PurgeAllScript = scriptFromResource("/purge_all_orders.lua")

  implicit val byteStringSerializer: ByteStringSerializer[CachedOrder] = new ByteStringSerializer[CachedOrder] {
    def serialize(data: CachedOrder): ByteString = {
      ByteString.fromArray(data.toByteArray)
    }
  }

  implicit val byteStringDeserializer: ByteStringDeserializer[CachedOrder] = new ByteStringDeserializer[CachedOrder] {
    def deserialize(data: ByteString): CachedOrder = {
      CachedOrder.parseFrom(data.toArray)
    }
  }

  override def getOrders(req: GetOrdersFromCache): Future[Map[String, CachedOrder]] = {
    Future.reduceLeft(
      groupedReq(req.orders)
        .map(kv ⇒ {
          redis.hmget(orderKey(kv._1), kv._2.map(_.orderHash): _*)
            .map(oc ⇒ oc.filter(_.isDefined).map(om ⇒ om.get.orderHash -> om.get).toMap)
        })
    )(_ ++ _)
  }

  override def purgeOrders(req: DelOrders): Future[Long] = {
    val bb = groupedReq(req.orders)
      .map(kv ⇒ redis.hdel(orderKey(kv._1), kv._2.map(_.orderHash): _*))
    Future.reduceLeft(
      groupedReq(req.orders)
        .map(kv ⇒ redis.hdel(orderKey(kv._1), kv._2.map(_.orderHash): _*))
    )(_ + _)
  }

  override def addOrUpdateOrders(req: SaveOrdersToCache): Future[Boolean] = {
    val grouped = groupedToCacheOrders(req)
    val rst = Future.reduceLeft(
      grouped.map(r ⇒ {
        redis.hmset(orderKey(r._1), r._2)
      })
    )(_ && _)

    grouped
      .values
      .flatMap(v ⇒ v.values)
      .filter(c ⇒ c.blockNumber > 0)
      .groupBy(c ⇒ c.blockNumber)
      .map(c ⇒ redis.sadd(blockKey(c._1), c._2.map(co ⇒ co.owner.toLowerCase + delimeter + co.orderHash.toLowerCase).toSeq: _*))

    rst
  }

  override def purgeAll(): Future[Long] = {
    delAll()
  }

  override def purgeAllForAddresses(owners: Seq[String]): Future[Long] = {
    redis.del(owners: _*)
  }

  override def purgeAllAfterBlock(forkFrom: Long, forkTo: Long): Future[Long] = {
    var result: Future[Long] = null
    Future.reduceLeft(
      (forkFrom to forkTo).map(i ⇒ redis.smembers[String](blockKey(i)))
    )(_ ++ _) onComplete {
        case Success(v) ⇒
          result = Future.reduceLeft(
            v.filter(c ⇒ c != null && !c.isEmpty)
              .map(c ⇒ unpackChangeInfo(c))
              .filter(s ⇒ s._1 != null && !s._2.isEmpty && s._2 != null && !s._2.isEmpty)
              .map(s ⇒ s._1 -> s._2).toMap
              .map(s ⇒ redis.hdel(orderKey(s._1), s._2))
          )(_ + _)
        case Failure(e) ⇒ result = Future.failed(e)
      }
    result
  }

  def unpackChangeInfo(changeInfo: String): (String, String) = {
    val splited = changeInfo.split(delimeter)
    (splited(0), splited(1))
  }

  def orderKey(owner: String): String = ORDER_CACHE_PRE + owner.toLowerCase
  def blockKey(blockNumber: Long): String = UPDATED_BY_BLOCK_PRE + blockNumber
  def groupedReq(ordersReq: Seq[OrderReq]): Map[String, Seq[OrderReq]] = {
    if (ordersReq == null || ordersReq.isEmpty) {
      Map.empty
    } else {
      ordersReq.filter(!_.owner.isEmpty).filter(!_.orderHash.isEmpty)
        .groupBy(o ⇒ o.owner)
    }
  }

  def groupedToCacheOrders(req: SaveOrdersToCache): Map[String, Map[String, CachedOrder]] = {
    if (req == null || req.orders.isEmpty) {
      Map.empty
    } else {
      req.orders.filter(!_.owner.isEmpty).filter(!_.orderHash.isEmpty)
        .groupBy(o ⇒ o.owner)
        .map(kv ⇒ kv._1 -> kv._2.map(o ⇒ o.orderHash -> o).toMap)
    }
  }
}
