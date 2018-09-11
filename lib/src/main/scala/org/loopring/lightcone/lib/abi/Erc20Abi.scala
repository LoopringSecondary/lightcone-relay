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
import org.loopring.lightcone.proto.block_chain_event.{ Approve, Transfer, TxHeader }

import scala.io.Source

class Erc20Abi @Inject() (config: Config) extends ContractAbi {

  val FN_APPROVE = "approve"
  val FN_TRANSFER = "transfer"
  val FN_TRANSFER_FROM = "transferFrom"

  val EN_APPROVAL = "Approval"
  val EN_TRANSFER = "Transfer"

  override def abi: Abi = {
    val path = config.getString("abi.basedir") + config.getString("abi.erc20")
    val str = Source.fromFile(path).getLines().map(_.trim).reduce(_ + _)
    Abi.fromJson(str)
  }

  override def supportedFunctions: Seq[String] = Seq(
    FN_APPROVE, FN_TRANSFER)
  override def supportedEvents: Seq[String] = Seq(
    EN_APPROVAL, EN_TRANSFER)

  val balanceOf: Abi.Function = abi.findFunction(predicate("balanceOf"))
  val allowance: Abi.Function = abi.findFunction(predicate("allowance"))

  def decodeInputAndAssemble(input: String, header: TxHeader): Seq[Any] = {
    val res = decodeInput(input)
    res.name match {
      case FN_TRANSFER => Seq(assembleTransferFunction(res.list, header))
      case FN_TRANSFER_FROM => Seq(assembleTransferFromFunction(res.list, header))
      case FN_APPROVE => Seq(assembleApproveFunction(res.list, header))
      case _ => Seq()
    }
  }

  def decodeLogAndAssemble(log: Log, header: TxHeader): Seq[Any] = {
    val res = decodeLog(log)
    res.name match {
      case EN_TRANSFER => Seq(assembleTransferEvent(res.list, header))
      case EN_APPROVAL => Seq(assembleApprovalEvent(res.list, header))
      case _ => Seq()
    }
  }

  def assembleTransferFunction(list: Seq[Any], header: TxHeader): Transfer = {
    if (list.length != 2) {
      throw new Exception("length of transfer function invalid")
    }

    val ret = Transfer()
      .withReceiver(scalaAny2Hex(list(0)))
      .withValue(scalaAny2Hex(list(1)))
      .withSender(header.from)
      .withTxHeader(header)

    print(ret.toProtoString)
    ret
  }

  def assembleTransferFromFunction(list: Seq[Any], header: TxHeader): Transfer = {
    if (list.length != 3) {
      throw new Exception("length of transfer from function invalid")
    }

    val ret = Transfer()
      .withSender(scalaAny2Hex(list(0)))
      .withReceiver(scalaAny2Hex(list(1)))
      .withValue(scalaAny2Hex(list(2)))
      .withTxHeader(header)

    print(ret)

    ret
  }

  def assembleTransferEvent(list: Seq[Any], header: TxHeader): Transfer = {
    if (list.length != 3) {
      throw new Exception("length of transfer event invalid")
    }

    val ret = Transfer()
      .withSender(scalaAny2Hex(list(0)))
      .withReceiver(scalaAny2Hex(list(1)))
      .withValue(scalaAny2Hex(list(2)))
      .withTxHeader(header)

    print(ret)

    ret
  }

  def assembleApproveFunction(list: Seq[Any], header: TxHeader): Approve = {
    if (list.length != 2) {
      throw new Exception("length of approve function invalid")
    }

    val ret = Approve()
      .withOwner(header.from)
      .withSpender(scalaAny2Hex(list(0)))
      .withValue(scalaAny2Bigint(list(1)).toString())
      .withTxHeader(header)

    print(ret.toProtoString)

    ret
  }

  def assembleApprovalEvent(list: Seq[Any], header: TxHeader): Approve = {
    if (list.length != 3) {
      throw new Exception("length of approve event invalid")
    }

    val ret = Approve()
      .withOwner(scalaAny2Hex(list(0)))
      .withSpender(scalaAny2Hex(list(1)))
      .withValue(scalaAny2Bigint(list(2)).toString())
      .withTxHeader(header)

    print(ret.toProtoString)

    ret
  }
}
