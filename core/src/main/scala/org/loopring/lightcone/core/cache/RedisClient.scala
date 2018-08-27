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

import org.apache.commons.lang3.SerializationUtils
import org.slf4j.LoggerFactory
import redis.clients.jedis._

import scala.collection.JavaConverters
import scala.collection.JavaConverters._

object RedisClient {

  case class RedisConfig()

  private val logger = LoggerFactory.getLogger(this.getClass)
  //TODO(xiaolu) move this to config file
  private var host = ""
  private var port = 0
  private var pool = new JedisPool(new JedisPoolConfig(), host, port)

  def initWith(host: String, port: Int): Unit =
    pool = new JedisPool(new JedisPoolConfig(), host, port)

  private def withClient[T](action: Jedis => T): T = {
    val jedis = pool.getResource
    try {
      action(jedis)
    } finally {
      jedis.close()
    }
  }

  def get[T <: Serializable](key: String): T = withClient { conn => fromByte[T](conn.get(key.getBytes())) }
  def set[T <: Serializable](key: String, v: T): String = withClient { conn => conn.set(key.getBytes, toByte(v)) }
  def del(key: String): Long = withClient { conn => conn.del(key) }
  def dels(keys: String*): Long = withClient { conn => conn.del(keys: _*) }
  def exist(key: String): Boolean = withClient { conn => conn.exists(key) }
  def hmset[T <: Serializable](key: String, valMap: Map[String, T]): String =
    withClient { conn =>
      val toByteMap = valMap.map(kv => (kv._1.getBytes, toByte(kv._2)))
      conn.hmset(key.getBytes(), JavaConverters.mapAsJavaMap(toByteMap))
    }
  def hmget[T <: Serializable](key: String, fields: String*): Seq[T] =
    withClient { conn =>
      val result: java.util.List[Array[Byte]] = conn.hmget(key.getBytes(), fields.map(_.getBytes): _*)
      JavaConverters.asScalaIteratorConverter(result.iterator).asScala.map(fromByte[T]).toSeq
    }

  def hgetAll[T <: Serializable](key: String): Map[String, T] =
    withClient { conn =>
      val getRst = conn.hgetAll(key.getBytes)
      getRst.asScala.map(kv => (kv._1.map(_.toChar).mkString, fromByte[T](kv._2))).toMap
    }

  def hset[T <: Serializable](key: String, field: String, value: T): Long = withClient { conn => conn.hset(key.getBytes, field.getBytes, toByte(value)) }
  def hexists[T <: Serializable](key: String, field: String): Boolean = withClient { conn => conn.hexists(key, field) }

  def zadd[T <: Serializable](key: String, score: Double, member: T): Long =
    withClient { conn =>
      conn.zadd(key.getBytes, score, toByte(member))
    }
  def zrange[T <: Serializable](key: String, start: Long, end: Long): Seq[T] =
    withClient { conn =>
      val rangeRst = conn.zrange(key.getBytes(), start, end)
      JavaConverters.asScalaIteratorConverter(rangeRst.iterator).asScala.map(fromByte[T]).toSeq
    }
  def zrem[T <: Serializable](key: String, members: T*): Long = withClient { conn => conn.zrem(key.getBytes, members.map(toByte): _*) }

  def srem[T <: Serializable](key: String, members: T*): Long = withClient { conn => conn.srem(key.getBytes, members.map(toByte): _*) }
  def sismember[T <: Serializable](key: String, member: T): Boolean = withClient { conn => conn.sismember(key.getBytes, toByte(member)) }
  def sadd[T <: Serializable](key: String, members: T*): Long = withClient { conn => conn.sadd(key.getBytes, members.map(toByte): _*) }
  def smembers[T <: Serializable](key: String): Set[T] =
    withClient { conn =>
      val rangeRst = conn.smembers(key.getBytes())
      JavaConverters.asScalaIteratorConverter(rangeRst.iterator).asScala.map(fromByte[T]).toSet
    }

  def expireAt(key: String, unixTime: Long) = withClient { conn => conn.expireAt(key, unixTime) }

  def toByte[T <: Serializable](value: T): Array[Byte] = SerializationUtils.serialize(value)
  def fromByte[T <: Serializable](value: Array[Byte]): T = SerializationUtils.deserialize[T](value)

}

