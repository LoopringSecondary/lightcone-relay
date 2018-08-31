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

import org.loopring.lightcone.lib.AbiData
import org.loopring.lightcone.lib.solidity.Abi
import org.spongycastle.util.encoders.Hex

case class AbiSupport() extends AbiData {

  def signature(e: Abi.Entry) = Hex.toHexString(e.encodeSignature()).toLowerCase()

  def findFunctionByName(name: String) = nameFuncMap(name)
  def findEventByName(name: String) = nameEvtMap(name)

  def findTransactionFunctionSig(txInput: String) = withoutPrefix(txInput).substring(0, 4)
  def findReceiptEventSig(firstTopic: String) = withoutPrefix(firstTopic)

  def isSupportedFunction(txInput: String) = sigFuncMap.contains(findTransactionFunctionSig(txInput))
  def isSupportedEvent(firstTopic: String) = sigEvtMap.contains(findReceiptEventSig(firstTopic))

  def decode(txinput: String): Seq[Any] = {
    val sig = findTransactionFunctionSig(txinput)
    if (sigFuncMap.contains(sig)) {
      val input = withoutPrefix(txinput).getBytes()
      sigFuncMap(sig).name match {
        case FN_SUBMIT_RING => Seq(ringSerializer.decode(input))
        case _ => Seq()
      }
    } else {
      Seq()
    }
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
      case true => src.substring(2)
      case false => src
    }
  }
}
