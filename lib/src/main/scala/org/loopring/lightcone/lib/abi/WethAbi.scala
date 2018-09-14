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

import org.loopring.lightcone.lib.solidity.Abi
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.proto.block_chain_event._

// "abi/weth.json"
class WethAbi(resourcePath: String)
  extends Erc20Abi(resourcePath) {

  val FN_DEPOSIT = "deposit"
  val FN_WITHDRAW = "withdraw"

  val EN_DEPOSIT = "Deposit"
  val EN_WITHDRAWAL = "Withdrawal"

  override def supportedFunctions: Seq[String] = {
    super.supportedFunctions.seq ++ Seq(FN_DEPOSIT, FN_WITHDRAW)
  }
  override def supportedEvents: Seq[String] = {
    super.supportedEvents.seq ++ Seq(EN_DEPOSIT, EN_WITHDRAWAL)
  }

  override def decodeInputAndAssemble(input: String, header: TxHeader): Seq[Any] = {
    val erc20seq = super.decodeInputAndAssemble(input, header)

    val res = decodeInput(input)
    val wethseq = res.name match {
      case FN_DEPOSIT  ⇒ Seq(assembleDepositFunction(res.list, header))
      case FN_WITHDRAW ⇒ Seq(assembleWithdrawalFunction(res.list, header))
      case _           ⇒ Seq()
    }

    erc20seq ++ wethseq
  }

  override def decodeLogAndAssemble(log: Log, header: TxHeader): Seq[Any] = {
    val erc20seq = super.decodeLogAndAssemble(log, header)

    val res = decodeLog(log)
    val wethseq = res.name match {
      case EN_DEPOSIT    ⇒ Seq(assembleDepositEvent(res.list, header))
      case EN_WITHDRAWAL ⇒ Seq(assembleWithdrawalEvent(res.list, header))
      case _             ⇒ Seq()
    }

    erc20seq ++ wethseq
  }

  // function deposit() public payable
  def assembleDepositFunction(list: Seq[Any], header: TxHeader): WethDeposit = {
    val ret = WethDeposit()
      .withOwner(header.from)
      .withValue(header.value)
      .withTxHeader(header)

    print(ret.toProtoString)

    ret
  }

  // event  Deposit(address indexed dst, uint wad);
  def assembleDepositEvent(list: Seq[Any], header: TxHeader): WethDeposit = {
    val ret = WethDeposit()
      .withOwner(scalaAny2Hex(list(0)))
      .withValue(scalaAny2Bigint(list(1)).toString())
      .withTxHeader(header)

    print(ret.toProtoString)

    ret
  }

  // function withdraw(uint wad) public
  def assembleWithdrawalFunction(list: Seq[Any], header: TxHeader): WethWithdrawal = {
    if (list.length != 1) {
      throw new Exception("withdrawal function list length invalid")
    }

    val ret = WethWithdrawal()
      .withOwner(header.from)
      .withValue(scalaAny2Bigint(list(0)).toString())
      .withTxHeader(header)

    print(ret)

    ret
  }

  // event  Withdrawal(address indexed src, uint wad);
  def assembleWithdrawalEvent(list: Seq[Any], header: TxHeader): WethWithdrawal = {
    if (list.length != 2) {
      throw new Exception("withdrawal event list length invalid")
    }

    val ret = WethWithdrawal()
      .withOwner(scalaAny2Hex(list(0)))
      .withValue(scalaAny2Bigint(list(1)).toString())
      .withTxHeader(header)

    print(ret)
    ret
  }

}
