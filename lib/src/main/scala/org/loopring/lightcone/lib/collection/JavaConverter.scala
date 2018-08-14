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

package org.loopring.lightcone.lib.collection

import java.lang.Object
import java.util
import java.util.ArrayList

import com.google.protobuf.ByteString
import org.loopring.lightcone.proto.lib.solidity.repeatedBytes

import scala.collection.JavaConverters

trait JavaConverter {
  def toSeqRepeatedBytes(src: Any): Seq[repeatedBytes]
}

final class SimpleJavaConverter extends JavaConverter {

  def toSeqRepeatedBytes(src: Any): Seq[repeatedBytes] = src match {
    case arr: Object => {
      arr.asInstanceOf[Array[Object]].toSeq.map(
        subarr => {
          repeatedBytes().withBytesList(subarr.asInstanceOf[Array[Object]].toSeq.map(x => x match {
            case obj: Object => ByteString.copyFrom(x.asInstanceOf[Array[Byte]])
            case _ => throw new Exception("to sub array[object] failed")
          }))
        })
    }
    case _ => throw new Exception("to array[object] failed")
  }
}
