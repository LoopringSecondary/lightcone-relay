/*
 * Copyright 2018 lightcore-relay
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
import java.lang.{ Boolean ⇒ jbool }
import org.apache.commons.collections4.Predicate
import org.loopring.lightcone.lib.solidity.Abi
import org.loopring.lightcone.proto.eth_jsonrpc.Log
import org.spongycastle.util.encoders.Hex

import scala.io.Source

abstract class ContractAbi(resourceFile: String) {
  def supportedFunctions: Seq[String]
  def supportedEvents: Seq[String]

  val prefix = "0x"
  val FunctionSigLength = 8

  def abi: Abi = Abi.fromJson(getAbiResource(resourceFile))

  def sigFuncMap: Map[String, Abi.Function] = {
    supportedFunctions.map(x ⇒ {
      val f = abi.findFunction(predicate(x))
      Hex.toHexString(f.encodeSignature()) -> f
    })
  }.toMap

  def sigEvtMap: Map[String, Abi.Event] = {
    supportedEvents.map(x ⇒ {
      val e = abi.findEvent(predicate(x))
      Hex.toHexString(e.encodeSignature()) -> e
    })
  }.toMap

  def nameFuncMap: Map[String, Abi.Function] = {
    supportedFunctions.map(x ⇒ {
      val f = abi.findFunction(predicate(x))
      f.name -> f
    })
  }.toMap

  def nameEvtMap: Map[String, Abi.Event] = {
    supportedEvents.map(x ⇒ {
      val e = abi.findEvent(predicate(x))
      e.name -> e
    })
  }.toMap

  def predicate[T <: Abi.Entry](name: String): Predicate[T] = (x) ⇒ x.name.equals(name)
  def signature[T <: Abi.Entry](e: T) = Hex.toHexString(e.encodeSignature()).toLowerCase()

  def findFunctionByName(name: String) = abi.findFunction(predicate(name))
  def findEventByName(name: String) = abi.findEvent(predicate(name))

  def findTransactionFunctionSig(txInput: String) = withoutPrefix(txInput).substring(0, FunctionSigLength)
  def findReceiptEventSig(firstTopic: String) = withoutPrefix(firstTopic)
  def isSupportedFunctionSig(sig: String) = sigFuncMap.contains(sig)
  def isSupportedEventSig(sig: String) = sigEvtMap.contains(sig)
  def findFunctionWithSig(sig: String) = sigFuncMap.get(sig)
  def findEventWithSig(sig: String) = sigEvtMap.get(sig)
  def isSupportedFunction(txInput: String) = sigFuncMap.contains(findTransactionFunctionSig(txInput))
  def isSupportedEvent(firstTopic: String) = sigEvtMap.contains(findReceiptEventSig(firstTopic))

  case class decodeResult(name: String = "", list: Seq[Any] = Seq())

  def decodeInput(txinput: String): decodeResult = {
    val sig = findTransactionFunctionSig(txinput)
    findFunctionWithSig(sig) match {
      case Some(abi) ⇒ {
        val decodedinput = getInputBytes(txinput)
        val list = abi.decode(decodedinput).toArray().toSeq
        decodeResult(abi.name, list)
      }
      case _ ⇒ decodeResult()
    }
  }

  def decodeLog(log: Log): decodeResult = {
    val sig = findReceiptEventSig(log.topics.head)
    findEventWithSig(sig) match {
      case Some(abi) ⇒ {
        val decodeddata = getLogDataBytes(log.data)
        val decodedtopics = log.topics.map(x ⇒ getLogDataBytes(x)).toArray
        val list = abi.decode(decodeddata, decodedtopics).toArray().toSeq
        decodeResult(abi.name, list)
      }
      case _ ⇒ decodeResult()
    }
  }

  def getInputBytes(input: String): Array[Byte] = {
    Hex.decode("00000000" + withoutPrefix(input).substring(FunctionSigLength))
  }

  def getLogDataBytes(data: String): Array[Byte] = {
    Hex.decode(withoutPrefix(data))
  }

  def getAbiResource(path: String): String = {
    val is = getClass.getClassLoader.getResourceAsStream(path)
    val source = Source.fromInputStream(is)
    val lines = source.getLines().toList
    lines.map(_.trim).reduce(_ + _)
  }

  private def withPrefix(src: String) = {
    val dst = src.toLowerCase()
    dst.startsWith(prefix) match {
      case true  ⇒ dst
      case false ⇒ prefix + dst
    }
  }

  private def withoutPrefix(src: String) = {
    val dst = src.toLowerCase()
    dst.startsWith(prefix) match {
      case true  ⇒ dst.substring(2)
      case false ⇒ dst
    }
  }

  // 这里比较特殊 涉及到任意类型的强制转换 只有abi转换时用到 所以放到该接口
  def javaObj2Hex(src: Object): String = src match {
    case bs: Array[Byte] ⇒ withPrefix(Hex.toHexString(bs))
    case _               ⇒ throw new Exception("java object convert to scala string error")
  }

  def javaObj2Bigint(src: Object): BigInt = src match {
    case bs: BigInteger ⇒ bs
    case _              ⇒ throw new Exception("java object convert to scala bigint error")
  }

  def javaObj2Boolean(src: Object): Boolean = src match {
    case b: jbool ⇒ b
    case _        ⇒ throw new Exception("java object convert to scala boolean error")
  }

  def scalaAny2Hex(src: Any): String = src match {
    case bs: Array[Byte] ⇒ withPrefix(Hex.toHexString(bs))
    case _               ⇒ throw new Exception("scala any convert to scala array byte error")
  }

  def scalaAny2Bigint(src: Any): BigInt = src match {
    case b: BigInteger ⇒ b
    case _             ⇒ throw new Exception("scala any convert to scala bigint error")
  }

  def scalaAny2Bool(src: Any): Boolean = src match {
    case b: Boolean ⇒ b
    case _          ⇒ throw new Exception("scala any convert to scala bool error")
  }
}
