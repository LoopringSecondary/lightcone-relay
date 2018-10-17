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

package org.loopring.lightcone.core.database.tables

import org.loopring.lightcone.core.database.base._
import org.loopring.lightcone.proto.{ ring ⇒ protoRing }
import slick.jdbc.MySQLProfile.api._

class Rings(tag: Tag) extends BaseTable[protoRing.Rings](tag, "Rings") {
  def feeRecipient = column[String]("fee_recipient", O.SqlType("VARCHAR(42)"))
  def miner = column[String]("miner", O.SqlType("VARCHAR(42)"))
  def sig = column[String]("sig", O.SqlType("VARCHAR(260)"))
  def signAlgorithm = column[String]("sign_algorithm", O.SqlType("VARCHAR(20)"))
  def hash = column[String]("hash", O.SqlType("VARCHAR(64)"))
  def nonce = column[Long]("nonce")
  def gasPrice = column[String]("gas_price", O.SqlType("VARCHAR(64)"))
  def gasLimit = column[String]("gas_limit", O.SqlType("VARCHAR(64)"))
  def contract = column[String]("contract", O.SqlType("VARCHAR(20)"))
  def value = column[String]("value", O.SqlType("VARCHAR(20)"))
  def inputData = column[String]("input_data", O.SqlType("text"))
  def txHash = column[String]("tx_hash", O.SqlType("VARCHAR(64)"))
  def status = column[Int]("status")
  def blockNumber = column[String]("block_number", O.SqlType("VARCHAR(64)"))
  def err = column[String]("err", O.SqlType("VARCHAR(100)"))

  def * = (
    feeRecipient,
    miner,
    sig,
    signAlgorithm,
    hash,
    nonce,
    gasPrice,
    gasLimit,
    contract,
    value,
    inputData,
    txHash,
    status,
    blockNumber,
    err
  ) <> (
      extendTupled,
      unwrapOption
    )

  private def extendTupled = (i: Tuple15[String, String, String, String, String, Long, String, String, String, String, String, String, Int, String, String]) ⇒
    protoRing.Rings()
      .withFeeRecipient(i._1)
      .withMiner(i._2)
      .withSig(i._3)
      .withSignAlgorithm(i._4)
      .withHash(i._5)
      .withNonce(i._6)
      .withGasPrice(i._7)
      .withGasLimit(i._8)
      .withContract(i._9)
      .withValue(i._10)
      .withInputData(i._11)
      .withTxHash(i._12)
      .withStatus(i._13)
      .withBlockNumber(i._14)
      .withErr(i._15)

  private def unwrapOption(rings: protoRing.Rings) = {
    Some(
      rings.feeRecipient,
      rings.miner,
      rings.sig,
      rings.signAlgorithm,
      rings.hash,
      rings.nonce,
      rings.gasPrice,
      rings.gasLimit,
      rings.contract,
      rings.value,
      rings.inputData,
      rings.txHash,
      rings.status,
      rings.blockNumber,
      rings.err
    )
  }

}
