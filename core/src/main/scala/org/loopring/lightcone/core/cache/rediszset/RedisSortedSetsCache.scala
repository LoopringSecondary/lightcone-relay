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

  //  def zaddWithOptions[V: ByteStringSerializer](key: String, options: Seq[ZaddOption], scoreMembers: (Double, V)*): Future[Long] =
  //    send(Zadd(key, options, scoreMembers))
  //
  //  def zcard(key: String): Future[Long] =
  //    send(Zcard(key))
  //
  def zcount[T](req: T, min: Double, max: Double)(implicit s: RedisSortedSetsSetSerializer[T]): Future[Long] =
    redis.zcount(s.cacheKey(req), Limit(min), Limit(max))
  //
  //  def zincrby[V: ByteStringSerializer](key: String, increment: Double, member: V): Future[Double] =
  //    send(Zincrby(key, increment, member))
  //
  //  def zinterstore
  //  (destination: String, key: String, keys: Seq[String], aggregate: Aggregate = SUM): Future[Long] =
  //    send(Zinterstore(destination, key, keys, aggregate))
  //
  //  def zinterstoreWeighted(destination: String, keys: Map[String, Double], aggregate: Aggregate = SUM): Future[Long] =
  //    send(ZinterstoreWeighted(destination, keys, aggregate))
  //
  //  def zrange[R: ByteStringDeserializer](key: String, start: Long, stop: Long): Future[Seq[R]] =
  //    send(Zrange(key, start, stop))
  //
  //  def zrangeWithscores[R: ByteStringDeserializer](key: String, start: Long, stop: Long): Future[Seq[(R, Double)]] =
  //    send(ZrangeWithscores(key, start, stop))
  //
  //  def zrangebyscore[R: ByteStringDeserializer](key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[R]] =
  //    send(Zrangebyscore(key, min, max, limit))
  //
  //  def zrangebyscoreWithscores[R: ByteStringDeserializer](key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[(R, Double)]] =
  //    send(ZrangebyscoreWithscores(key, min, max, limit))
  //
  //  def zrank[V: ByteStringSerializer](key: String, member: V): Future[Option[Long]] =
  //    send(Zrank(key, member))
  //
  //  def zrem[V: ByteStringSerializer](key: String, members: V*): Future[Long] =
  //    send(Zrem(key, members))
  //
  //  def zremrangebylex(key: String, min: String, max: String): Future[Long] =
  //    send(Zremrangebylex(key, min, max))
  //
  //  def zremrangebyrank(key: String, start: Long, stop: Long): Future[Long] =
  //    send(Zremrangebyrank(key, start, stop))
  //
  //  def zremrangebyscore(key: String, min: Limit, max: Limit): Future[Long] =
  //    send(Zremrangebyscore(key, min, max))
  //
  //  def zrevrange[R: ByteStringDeserializer](key: String, start: Long, stop: Long): Future[Seq[R]] =
  //    send(Zrevrange(key, start, stop))
  //
  //  def zrevrangeWithscores[R: ByteStringDeserializer](key: String, start: Long, stop: Long): Future[Seq[(R, Double)]] =
  //    send(ZrevrangeWithscores(key, start, stop))
  //
  //  def zrevrangebyscore[R: ByteStringDeserializer](key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[R]] =
  //    send(Zrevrangebyscore(key, min, max, limit))
  //
  //  def zrevrangebyscoreWithscores[R: ByteStringDeserializer](key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[(R, Double)]] =
  //    send(ZrevrangebyscoreWithscores(key, min, max, limit))
  //
  //  def zrevrank[V: ByteStringSerializer](key: String, member: V): Future[Option[Long]] =
  //    send(Zrevrank(key, member))
  //
  //  def zscore[V: ByteStringSerializer](key: String, member: V): Future[Option[Double]] =
  //    send(Zscore(key, member))
  //
  //  def zunionstore
  //  (destination: String, key: String, keys: Seq[String], aggregate: Aggregate = SUM): Future[Long] =
  //    send(Zunionstore(destination, key, keys, aggregate))
  //
  //  def zunionstoreWeighted
  //  (destination: String, keys: Map[String, Double], aggregate: Aggregate = SUM): Future[Long] =
  //    send(ZunionstoreWeighted(destination, keys, aggregate))
  //
  //  def zrangebylex[R: ByteStringDeserializer](key: String, min: Option[String], max: Option[String], limit: Option[(Long, Long)] = None): Future[Seq[R]] =
  //    send(Zrangebylex(key, min.getOrElse("-"), max.getOrElse("+"), limit))
  //
  //  def zrevrangebylex[R: ByteStringDeserializer](key: String, max: Option[String], min: Option[String], limit: Option[(Long, Long)] = None): Future[Seq[R]] =
  //    send(Zrevrangebylex(key, max.getOrElse("+"), max.getOrElse("-"), limit))
  //
  //  def zscan[R:RedisSortedSetsGetSerializer](key: String, cursor: Int = 0, count: Option[Int] = None, matchGlob: Option[String] = None)(implicit s: RedisSortedSetsGetSerializer[R]): Future[Cursor[Seq[(Double, R)]]] =
  //    redis.zscan[Array[Byte]](key, cursor, count, matchGlob)
}
