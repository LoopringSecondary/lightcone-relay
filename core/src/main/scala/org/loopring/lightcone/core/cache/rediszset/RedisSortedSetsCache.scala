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

package org.loopring.lightcone.core.cache.rediszset

import redis.{ Cursor, RedisCluster }
import redis.api.Limit

import scala.concurrent.{ ExecutionContext, Future }

trait RedisSortedSetsCache {
  val redis: RedisCluster
  implicit val ec: ExecutionContext

  def zadd[T](req: T)(implicit s: RedisSortedSetsSetSerializer[T]): Future[Long] = {
    val scoreMembers = Seq[(Double, Array[Byte])]()
    redis.zadd[Array[Byte]](s.cacheKey(req), scoreMembers: _*)
  }
  def zcount[T](req: T, min: Double, max: Double)(implicit s: RedisSortedSetsSetSerializer[T]): Future[Long] =
    redis.zcount(s.cacheKey(req), Limit(min), Limit(max))
}
