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

import scala.concurrent._

trait Cache[K, V] {
  implicit val ex: ExecutionContext
  def get(key: K): Future[Option[V]]
  def get(keys: Seq[K]): Future[Map[K, V]]
  def put(key: K, value: V): Future[Any]

  def put(keyValues: Map[K, V]): Future[Any] =
    Future.sequence(
      keyValues.map {
        case (k, v) => put(k, v)
      }.toSeq)
}

trait ByteArrayCache
  extends Cache[Array[Byte], Array[Byte]]

// this ByteArrayRedisCache needs to be final class, not a trait
trait ByteArrayRedisCache extends ByteArrayCache

trait ProtoCache[K, V <: scalapb.GeneratedMessage with scalapb.Message[V]]
  extends Cache[K, V] {
  implicit val ex: ExecutionContext

  val underlying: ByteArrayCache
  val serializer: ProtoSerializer[V]

  def keyToBytes(k: K): Array[Byte]

  def get(key: K): Future[Option[V]] = {
    underlying.get(keyToBytes(key))
      .map(_.map(serializer.fromBytes))
  }
  def get(keys: Seq[K]): Future[Map[K, V]] = {
    val keyMap = keys.map(k => keyToBytes(k) -> k).toMap
    underlying.get(keyMap.keys.toSeq).map {
      _.map {
        case (k, v) => keyMap(k) -> serializer.fromBytes(v)
      }
    }
  }
  def put(key: K, value: V) = {
    underlying.put(
      keyToBytes(key),
      serializer.toBytes(value))
  }
}

final class StringToProtoCache[V <: scalapb.GeneratedMessage with scalapb.Message[V]](
  val underlying: ByteArrayCache,
  val serializer: ProtoSerializer[V])(implicit val ex: ExecutionContext)
  extends ProtoCache[String, V] {
  def keyToBytes(str: String) = str.getBytes
}

final class ProtoToProtoCache[R <: scalapb.GeneratedMessage with scalapb.Message[R], V <: scalapb.GeneratedMessage with scalapb.Message[V]](
  val underlying: ByteArrayCache,
  val serializer: ProtoSerializer[V],
  val genKey: R => Array[Byte])(implicit val ex: ExecutionContext)
  extends ProtoCache[R, V] {
  def keyToBytes(key: R) = genKey(key)
}
