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
import org.loopring.lightcone.proto.example._

private object ReaderExample {
  class Example1Reader extends Reader[Example1Req, Example1Resp] {
    def read(req: Example1Req) = ??? // read from ethereum
    def read(reqs: Seq[Example1Req]) = ??? // read from ethereum
  }

  class Example2Reader extends Reader[String, Example2Resp] {
    def read(req: String) = ??? // read from ethereum
    def read(reqs: Seq[String]) = ??? // read from ethereum
  }

  implicit val cache: ByteArrayRedisCache = ???
  import scala.concurrent.ExecutionContext.Implicits.global

  val reader1 = new ProtoToProtoCachedReader(
    new Example1Reader,
    (req: Example1Req) => req.id.getBytes)

  val reader2 = new StringToProtoCachedReader(new Example2Reader)

  // now reader1 and reader2 will read from cache, if not found, will read
  // from ethreum by using raw1Reader and raw2Reader
  reader1.read(Example1Req("123"))
  reader2.read(Seq("1", "2"))
}
