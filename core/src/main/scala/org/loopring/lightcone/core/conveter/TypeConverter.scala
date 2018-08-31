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

package org.loopring.lightcone.core.conveter

import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.lang.{ Boolean => jbool }

trait TypeConverter {

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

