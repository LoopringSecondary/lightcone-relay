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

package org.loopring.lightcone.abi

import org.apache.commons.collections4.Predicate
import org.loopring.lightcone.lib.solidity.Abi
import org.spongycastle.util.encoders.Hex

trait AbiSupport {
  implicit val abimap: Map[String, String]

  val (sigFunctionMap: Map[String, Abi.Function],
    sigEventMap: Map[String, Abi.Event],
    nameFunctionMap: Map[String, Abi.Function],
    nameEventMap: Map[String, Abi.Event]) = {

    var fmap: Map[String, Abi.Function] = Map()
    var emap: Map[String, Abi.Event] = Map()
    var nfmap: Map[String, Abi.Function] = Map()
    var nemap: Map[String, Abi.Event] = Map()

    abimap.map(x => {
      val abi = Abi.fromJson(x._2)
      val iterator = abi.iterator()
      while (iterator.hasNext) {
        iterator.next() match {
          case f: Abi.Function =>
            val sig = signature(f)
            println(s"abi ${x._1} function ${f.name} --> ${sig}")
            fmap += sig -> f
            nfmap += f.name -> f
          case e: Abi.Event =>
            val sig = signature(e)
            println(s"abi ${x._1} event ${e.name} --> ${sig}")
            emap += sig -> e
            nemap += e.name -> e
        }
      }
    })

    (fmap, emap, nfmap, nemap)
  }

  def findFunctionByName(name: String) = nameFunctionMap(name)
  def findEventByName(name: String) = nameEventMap(name)
  def findTransactionFunctionSig(txInput: String) = withoutPrefix(txInput).substring(0, 4).toLowerCase()
  def findReceiptEventSig(firstTopic: String) = withoutPrefix(firstTopic).toLowerCase()
  def isSupportedFunction(txInput: String) = sigFunctionMap.contains(findTransactionFunctionSig(txInput))
  def isSupportedEvent(firstTopic: String) = sigEventMap.contains(findReceiptEventSig(firstTopic))

  private def signature(e: Abi.Entry) =
    withPrefix(Hex.toHexString(e.encodeSignature()).toLowerCase())

  val prefix = "0x"

  private def withPrefix(src: String) = src.toLowerCase().startsWith(prefix) match {
    case true => src
    case false => (prefix + src).toLowerCase()
  }

  private def withoutPrefix(src: String) = src.toLowerCase().startsWith(prefix) match {
    case true => src.substring(2).toLowerCase()
    case false => src
  }
}
