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
import java.lang.{ Boolean => jbool }

import org.apache.commons.collections4.Predicate
import org.loopring.lightcone.lib.solidity.Abi
import org.loopring.lightcone.proto.eth_jsonrpc.Log
import org.spongycastle.util.encoders.Hex

trait ContractAbi {
  val prefix = "0x"
  val FunctionSigLength = 8

  val abi: Abi

  val supportedFunctions: Seq[String] = Seq()
  val supportedEvents: Seq[String] = Seq()

  val (
    sigFuncMap: Map[String, Abi.Function],
    sigEvtMap: Map[String, Abi.Event],
    nameFuncMap: Map[String, Abi.Function],
    nameEvtMap: Map[String, Abi.Event]
    ) = {
    var fmap: Map[String, Abi.Function] = Map()
    var emap: Map[String, Abi.Event] = Map()
    var nfmap: Map[String, Abi.Function] = Map()
    var nemap: Map[String, Abi.Event] = Map()

    supportedFunctions.map(x => {
      val f = abi.findFunction(predicate(x))
      fmap += signature(f) -> f
      nfmap += x -> f
    })

    supportedEvents.map(x => {
      val e = abi.findEvent(predicate(x))
      emap += signature(e) -> e
      nemap += x -> e
    })

    (fmap, emap, nfmap, nemap)
  }

  def predicate[T <: Abi.Entry](name: String): Predicate[T] = (x) => x.name.equals(name)
  def signature[T <: Abi.Entry](e: T) = Hex.toHexString(e.encodeSignature()).toLowerCase()

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

  case class decodeResult(name: String = "", list: Seq[Any] = Seq())

  def decodeInput(txinput: String): decodeResult = {
    val sig = findTransactionFunctionSig(txinput)
    if (isSupportedFunctionSig(sig)) {
      val decodedinput = getInputBytes(txinput)
      val abi = findFunctionWithSig(sig)
      val list = abi.decode(decodedinput).toArray().toSeq
      decodeResult(abi.name, list)
    } else {
      decodeResult()
    }
  }

  def decodeLog(log: Log): decodeResult = {
    val sig = findReceiptEventSig(log.topics.head)
    if (isSupportedEventSig(sig)) {
      val decodeddata = getLogDataBytes(log.data)
      val decodedtopics = log.topics.map(x => getLogDataBytes(x)).toArray
      val abi = findEventWithSig(sig)
      val list = abi.decode(decodeddata, decodedtopics).toArray().toSeq
      decodeResult(abi.name, list)
    } else {
      decodeResult()
    }
  }

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

  // 这里比较特殊 涉及到任意类型的强制转换 只有abi转换时用到 所以放到该接口
  def javaObj2Hex(src: Object): String = src match {
    case bs: Array[Byte] => Hex.toHexString(bs)
    case _ => throw new Exception("java object convert to scala string error")
  }

  def javaObj2Bigint(src: Object): BigInt = src match {
    case bs: BigInteger => bs
    case _ => throw new Exception("java object convert to scala bigint error")
  }

  def javaObj2Boolean(src: Object): Boolean = src match {
    case b: jbool => b
    case _ => throw new Exception("java object convert to scala boolean error")
  }

  def scalaAny2Hex(src: Any): String = src match {
    case bs: Array[Byte] => Hex.toHexString(bs)
    case _ => throw new Exception("scala any convert to scala array byte error")
  }

  def scalaAny2Bigint(src: Any): BigInt = src match {
    case b: BigInteger => b
    case _ => throw new Exception("scala any convert to scala bigint error")
  }
}
