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

package org.loopring.lightcone.lib.abi

import com.google.inject.Inject
import com.typesafe.config.Config
import org.loopring.lightcone.lib.solidity.Abi
import org.loopring.lightcone.proto.eth_jsonrpc.Log
import org.loopring.lightcone.proto.block_chain_event.{ Transfer, TxHeader }

class Erc20Abi @Inject() (config: Config) extends ContractAbi {

  val abi = Abi.fromJson(config.getString("abi.erc20"))

  val FN_APPROVE = "approve"
  val FN_TRANSFER = "transfer"

  val EN_APPROVAL = "Approval"
  val EN_TRANSFER = "Transfer"

  override val supportedFunctions: Seq[String] = Seq(
    FN_APPROVE, FN_TRANSFER)

  override val supportedEvents: Seq[String] = Seq(
    EN_APPROVAL, EN_TRANSFER)

  def decodeInputAndAssemble(input: String, header: TxHeader): Seq[Any] = {
    val res = decodeInput(input)
    res.name match {
      case FN_TRANSFER => Seq(assembleTransferFn(res.list, header))
      case _ => Seq()
    }
  }

  def decodeLogAndAssemble(log: Log, header: TxHeader): Seq[Any] = {
    val res = decodeLog(log)
    res.name match {
      case EN_TRANSFER => Seq(assembleTransferEn(res.list, header))
      case _ => Seq()
    }
  }

  def assembleTransferFn(list: Seq[Any], header: TxHeader): Transfer = {
    if (list.length != 3) {
      throw new Exception("length of transfer event invalid")
    }

    Transfer()
      .withReceiver(scalaAny2Hex(list(0)))
      .withSender(scalaAny2Hex(list(1)))
      .withValue(scalaAny2Hex(list(2)))
      .withTxHeader(header)
  }

  // todo
  def assembleTransferFromFn(list: Seq[Any], header: TxHeader): Transfer = ???

  // todo
  def assembleTransferEn(list: Seq[Any], header: TxHeader): Transfer = {
    Transfer().withTxHeader(header)
  }
}
