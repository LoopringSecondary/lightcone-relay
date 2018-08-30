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

trait Reader[R, T] {
  def read(req: R): Future[Option[T]]
  def read(reqs: Seq[R]): Future[Map[R, T]]
}

trait CachedReader[R, T] extends Reader[R, T] {
  implicit val ex: ExecutionContext
  val underlying: Reader[R, T]
  val cache: Cache[R, T]

  def read(req: R): Future[Option[T]] = for {
    cached <- cache.get(req)
    result <- cached match {
      case t @ Some(_) => Future(t)
      case None => underlying.read(req)
    }
  } yield result

  def read(reqs: Seq[R]): Future[Map[R, T]] = for {
    cached <- cache.get(reqs)
    cachedReqs = cached.keys.toSeq
    uncachedReqs = reqs.filter(r => !cachedReqs.contains(r))
    uncached <- underlying.read(uncachedReqs)
    _ <- cache.put(uncached)
    result = uncached ++ cached
  } yield result
}

final class StringToProtoCachedReader[T <: scalapb.GeneratedMessage with scalapb.Message[T]]()(
  implicit
  val ex: ExecutionContext,
  val underlying: Reader[String, T],
  val underlyingCache: ByteArrayCache,
  c: scalapb.GeneratedMessageCompanion[T])
  extends CachedReader[String, T] {

  val cache = new StringToProtoCache[T](
    underlyingCache,
    new ProtoSerializer)
}

final class ProtoToProtoCachedReader[R <: scalapb.GeneratedMessage with scalapb.Message[R], T <: scalapb.GeneratedMessage with scalapb.Message[T]](
  genKey: R => Array[Byte])(
  implicit
  val ex: ExecutionContext,
  val underlying: Reader[R, T],
  val underlyingCache: ByteArrayCache,
  cR: scalapb.GeneratedMessageCompanion[R],
  cT: scalapb.GeneratedMessageCompanion[T])
  extends CachedReader[R, T] {

  val cache = new ProtoToProtoCache[R, T](
    underlyingCache,
    new ProtoSerializer,
    genKey)
}

import org.loopring.lightcone.proto.example._

object Example {
  class Example1Reader extends Reader[Example1Req, Example1Resp] {
    def read(req: Example1Req) = ??? // read from ethereum
    def read(reqs: Seq[Example1Req]) = ??? // read from ethereum
  }

  class Example2Reader extends Reader[Example2Req, Example2Resp] {
    def read(req: Example2Req) = ??? // read from ethereum
    def read(reqs: Seq[Example2Req]) = ??? // read from ethereum
  }

  implicit val raw1Reader = new Example1Reader
  implicit val raw2Reader = new Example2Reader

  implicit val cache: ByteArrayCache = ???

  import scala.concurrent.ExecutionContext.Implicits.global
  val reader1 = new ProtoToProtoCachedReader[Example1Req, Example1Resp](_.id.getBytes)
  val reader2 = new ProtoToProtoCachedReader[Example2Req, Example2Resp](_.id.getBytes)

  // now reader1 and reader2 will read from cache, if not found, will read
  // from ethreum by using raw1Reader and raw2Reader
  reader1.read(Example1Req("123"))
  reader2.read(Seq(Example2Req("1"), Example2Req("2")))
}
