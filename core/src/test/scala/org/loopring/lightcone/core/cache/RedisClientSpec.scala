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

import org.joda.time.DateTime
import org.scalatest.{ FlatSpec, Matchers }

class RedisClientSpec extends FlatSpec with Matchers {

  info("execute get cmd to test redis conn")

  val testMessage = TestMessage("abc", 2)
  val testMessage2 = TestMessage("cde", 3)
  val testMessage3 = TestMessage("ghi", 4)

  RedisClient.initWith("18.182.39.62", 6379)

  "set redis cache" should "result is ok" in {
    val aa = RedisClient.set[TestMessage]("sss", testMessage)
    aa should be("OK")
  }

  "get redis cache" should "return correct cached result" in {
    val aa = RedisClient.get[TestMessage]("sss")
    aa.a should be("abc")
    aa.b should be(2)
  }

  "del redis key" should "del correctly" in {
    RedisClient.set[TestMessage]("toDel", testMessage)
    RedisClient.set[TestMessage]("toDel2", testMessage)
    val delRst = RedisClient.del("toDel")
    RedisClient.set[TestMessage]("toDel", testMessage)
    val delRst2 = RedisClient.dels("toDel", "toDel2")
    delRst should be(1)
    delRst2 should be(2)
  }

  "redis method exist" should "return correct result" in {
    RedisClient.set[TestMessage]("toDel", testMessage)
    RedisClient.exist("toDel") should be(true)
    RedisClient.exist("testExist") should be(false)
  }

  "redis method with hm head's test cases " should "return correct result" in {
    val hmTestKey = "hmsettest"
    val hmTestField1 = "hmset1"
    val hmTestField2 = "hmset2"
    RedisClient.del(hmTestKey)
    val setRst = RedisClient.hmset[TestMessage](hmTestKey, Map[String, TestMessage](hmTestField1 -> testMessage, hmTestField2 -> testMessage2))

    val rsts = RedisClient.hmget[TestMessage](hmTestKey, hmTestField1, hmTestField2)
    setRst should be("OK")
    rsts(0).a should be("abc")
    rsts(0).b should be(2)
    rsts(1).a should be("cde")
    rsts(1).b should be(3)

    val getAllRst = RedisClient.hgetAll[TestMessage](hmTestKey)
    getAllRst(hmTestField1).a should be("abc")
    getAllRst(hmTestField1).b should be(2)
    getAllRst(hmTestField2).a should be("cde")
    getAllRst(hmTestField2).b should be(3)
  }

  "redis method with h head's test cases" should "return correct result" in {
    val hsetKey = "hsettest"
    val hsetField = "tm"
    RedisClient.del(hsetKey)
    val setRst = RedisClient.hset[TestMessage](hsetKey, hsetField, testMessage)
    setRst should be(1)
    RedisClient.hexists(hsetKey, hsetField) should be(true)
  }

  "redis method with z head's test cases " should "return correct result" in {
    val zTestKey = "ztestKey"
    val zTestField1 = "zset1"
    val zTestField2 = "zset2"
    RedisClient.del(zTestKey)
    val zaddRst = RedisClient.zadd[TestMessage](zTestKey, 0.1, testMessage)
    zaddRst should be(1)
    RedisClient.zadd[TestMessage](zTestKey, 0.3, testMessage3)
    RedisClient.zadd[TestMessage](zTestKey, 0.2, testMessage2)

    val rsts = RedisClient.zrange[TestMessage](zTestKey, 1, 2)
    rsts.size should be(2)
    rsts(0).a should be("cde")
    rsts(0).b should be(3)
    rsts(1).a should be("ghi")
    rsts(1).b should be(4)

    val zremRst = RedisClient.zrem[TestMessage](zTestKey, testMessage, testMessage3)
    zremRst should be(2)
    val rsts2 = RedisClient.zrange[TestMessage](zTestKey, 0, 0)
    rsts2.size should be(1)
    rsts2.head.a should be("cde")
    rsts2.head.b should be(3)
  }

  "redis method with s head's test cases " should "return correct result" in {
    val sTestKey = "stestKey"
    val zTestField1 = "zset1"
    val zTestField2 = "zset2"
    RedisClient.del(sTestKey)
    val saddRst = RedisClient.sadd[TestMessage](sTestKey, testMessage, testMessage2, testMessage3)
    saddRst should be(3)
    RedisClient.sismember[TestMessage](sTestKey, testMessage) should be(true)
    RedisClient.sismember[TestMessage](sTestKey, testMessage2) should be(true)
    RedisClient.sismember[TestMessage](sTestKey, testMessage3) should be(true)

    val sremRst = RedisClient.srem(sTestKey, testMessage2, testMessage3)
    sremRst should be(2)
    RedisClient.sismember[TestMessage](sTestKey, testMessage2) should be(false)
    RedisClient.sismember[TestMessage](sTestKey, testMessage3) should be(false)

    RedisClient.sadd[TestMessage](sTestKey, testMessage, testMessage2, testMessage3)

    val rsts = RedisClient.smembers[TestMessage](sTestKey)
    rsts.size should be(3)
    rsts.contains(testMessage) should be(true)
    rsts.contains(testMessage2) should be(true)
    rsts.contains(testMessage3) should be(true)

  }

  "redis method expire test" should "expired correctly" in {
    val expireKey = "expireTest"
    RedisClient.del(expireKey)
    val setRst = RedisClient.set[TestMessage](expireKey, testMessage)
    setRst should be("OK")
    val now = DateTime.now().toDate.getTime / 1000
    RedisClient.expireAt(expireKey, 1535355477) should be(1)
  }

}

case class TestMessage(a: String, b: Int) extends Serializable
