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

package org.loopring.lightcone.lib.solidity

//import org.loopring.lightcone.lib.abi.LoopringAbi
//import org.loopring.lightcone.proto.block_chain_event.{ RingMined, SubmitRing, TxHeader }
//import org.loopring.lightcone.proto.eth_jsonrpc.Log
import com.typesafe.config.{ Config, ConfigFactory }
import org.loopring.lightcone.gateway.mybatis.{ Conn, DB, DBSampleData, DBSchema }
import org.loopring.lightcone.gateway.mybatis.dal.ApiSecretDao
import org.loopring.lightcone.gateway.mybatis.table.ApiSecret
import org.scalatest.FlatSpec

class MybatisSpec extends FlatSpec {

  info("execute cmd [sbt lib/'testOnly *MybatisSpec'] to test single spec of dbconn")

  def printTest(secret: ApiSecret): Unit = {
    println("========== start print secret")
    println(secret.id)
    println(secret.firstName)
    println(secret.lastName)
    println("========== end print secret")
  }

  val config = ConfigFactory.parseString(
    s"""
            db.mybatis.driver="com.mysql.jdbc.Driver"
            db.mybatis.url="jdbc:mysql://13.112.62.24:3306/lightcone_relay"
            db.mybatis.name="root"
            db.mybatis.password="111111"
            """
  )

  val conn = new Conn(config)

  conn.context.transaction { implicit session ⇒
    ApiSecretDao.initdb
    ApiSecretDao.initData
    //        ApiSecretDao insert ApiSecret(10, "1111", "2222")

    ApiSecretDao.findAll() foreach (printTest(_))

    //    DBSchema.create
    //    DBSampleData.populate
    //
    //    DBSampleData.findAll("a") foreach (p ⇒ {
    //      println(p.id)
    //      println(p.firstName)
    //      println(p.lastName)
    //    })
  }

  //
  //  "ringminedEvent" should "" in {
  //    val event = abi.findEventByName("RingMined")
  //    val txhash = "0x4cee545b2c91dda26aab4f595040a0f6db85edb0d88f688334c340754db76fb4"
  //    val data = "0x00000000000000000000000000000000000000000000000000000000000010960000000000000000000000005552dcfba48c94544beaaf26470df9898e050ac20000000000000000000000005552dcfba48c94544beaaf26470df9898e050ac20000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000e1dbacf6f900f075309a6cf6c5047eefe07e390b7253b24c0bc0cca341b31a446000000000000000000000000c3f2cfa6ac8c941fbc5c99119ef5125402853cbb000000000000000000000000ef68e7c694f40c8202821edf525de3782458639f000000000000000000000000000000000000000000000004d8021297c2b400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000027ae25757f1000000000000000000000000000000000000000000000000000000000000000000005553dc49215a7360d9d56c18ae9c67e9b5ab9dd3ea1732953558e9684109756e000000000000000000000000d1cbb547b88f019a41e2efeb60c07fb79e79acfa000000000000000000000000beb6fdf4ef6ceb975157be43cbe0047b248a89220000000000000000000000000000000000000000000000212d34c57c2b10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000fffffffffffffffffffffffffffffffffffffffffffffffff3828ec4b6260000"
  //    val topics = Seq(
  //      "0x4d2a4adf7c5f6cf35d97aecc1919897bf86299dccd9b5e19b2b38ebebf07add0",
  //      "0x70eaa81b86e8b4c82f9a72f0a0985f09def1035a451afb151ebb297364060c68"
  //    )
  //    val log = Log().withData(data).withTopics(topics)
  //
  //    abi.decodeLogAndAssemble(log, TxHeader()).map(x ⇒ x match {
  //      case s: RingMined ⇒ s.fills.map(f ⇒ info(f.toProtoString))
  //      case _            ⇒ info("unpack failed")
  //    })
  //
  //  }

}
