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
import org.spongycastle.util.encoders.Hex

case class AbiSupporter() extends AbiData {

  val prefix = "0x"
  val FunctionSigLength = 8

  val FN_SUBMIT_RING = "submitRing"
  val EV_RING_MINED = "RingMined"

  val FN_APPROVE = "approve"
  val FN_TRANSFER = "transfer"
  val FN_TRANSFER_FROM = "transferFrom"
  val EV_TRANSFER = "Transfer"
  val EV_APPROVAL = "Approval"

  def signature(e: Abi.Entry) = Hex.toHexString(e.encodeSignature()).toLowerCase()

  def findFunctionByName(name: String) = nameFuncMap(name)
  def findEventByName(name: String) = nameEvtMap(name)

  def findTransactionFunctionSig(txInput: String) = withoutPrefix(txInput).substring(0, FunctionSigLength)
  def findReceiptEventSig(firstTopic: String) = withoutPrefix(firstTopic)
  def isSupportedFunctionSig(sig: String) = sigFuncMap.contains(sig)
  def isSupportedEventSig(sig: String) = sigEvtMap.contains(sig)
  def findFunctionWithSig(sig: String) = sigFuncMap(sig)
  def findEventWithSig(sig: String) = sigEvtMap(sig)
  def isSupportedFunction(txInput: String) = sigFuncMap.contains(findTransactionFunctionSig(txInput))
  def isSupportedEvent(firstTopic: String) = sigEvtMap.contains(findReceiptEventSig(firstTopic))

  def getInputBytes(input: String): Array[Byte] = {
    Hex.decode("00000000" + withoutPrefix(input).substring(FunctionSigLength))
  }

  def getLogDataBytes(data: String): Array[Byte] = {
    Hex.decode(withoutPrefix(data))
  }

  private def withPrefix(src: String) = {
    val dst = src.toLowerCase()
    dst.startsWith(prefix) match {
      case true => dst
      case false => prefix + dst
    }
  }

  private def withoutPrefix(src: String) = {
    val dst = src.toLowerCase()
    dst.startsWith(prefix) match {
      case true => dst.substring(2)
      case false => dst
    }
  }

}
