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

import java.math.BigInteger

import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.proto.block_chain_event._
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.proto.order._
import org.loopring.lightcone.proto.ring.Ring

class RingSubmitterAbi(resourceFile: String)
  extends ContractAbi(resourceFile) {

  val FN_SUBMIT_RINGS = "submitRings"
  val EN_RING_MINED = "RingMined"

  def supportedFunctions: Seq[String] = Seq(
    FN_SUBMIT_RINGS,
  )

  def supportedEvents: Seq[String] = Seq(
    EN_RING_MINED,
  )

  def decodeInputAndAssemble(input: String, header: TxHeader): Seq[Any] = {
    val res = decodeInput(input)
    res.name match {
      case FN_SUBMIT_RINGS  ⇒ Seq(assembleSubmitRingFunction(res.list, header))
      case _               ⇒ Seq()
    }
  }

  def decodeLogAndAssemble(log: Log, header: TxHeader): Seq[Any] = {
    val res = decodeLog(log)
    res.name match {
      case EN_RING_MINED      ⇒ Seq(assembleRingminedEvent(res.list, header))
      case _                  ⇒ Seq()
    }
  }

  def assembleSubmitRingFunction(list: Seq[Any], header: TxHeader): SubmitRing = {
    SubmitRing()
  }

  // todo: safeBig处理负数还是有点问题
  def assembleRingminedEvent(list: Seq[Any], header: TxHeader): RingMined = {
    RingMined()
  }

  // 对负数异或取反，这两个方法只有该文件用到
  def safeBig(bytes: Array[Byte]): BigInt = {
    if (bytes(0) > 128) {
      new BigInteger(bytes).xor(maxUint256).not()
    } else {
      BigInt(bytes)
    }
  }

  val maxUint256: BigInteger = {
    val bytes = (1 to 32).map(_ ⇒ Byte.MaxValue).toArray
    new BigInteger(bytes)
  }
}
