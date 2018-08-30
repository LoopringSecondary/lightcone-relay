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

package org.loopring.lightcone.core.actors

import org.apache.commons.collections4.Predicate
import org.loopring.lightcone.lib.solidity.Abi

trait ExtractorSupport {
  implicit val abimap: Map[String, String]

  lazy val erc20Abi = Abi.fromJson(abimap("erc20"))
  lazy val implAbi = Abi.fromJson(abimap("impl"))

  val functions: Map[String, Abi.Function] = Map(Seq(
    findFunction(erc20Abi, "transfer"),
    findFunction(erc20Abi, "transferFrom"),
    findFunction(erc20Abi, "approve"),

    findFunction(implAbi, "submitRing"),
    findFunction(implAbi, "")).map { a => functionId(a) -> a }: _*)

  private def findFunction(abi: Abi, name: String) = {
    val predicate: Predicate[Abi.Function] = (x) => x.name.equals(name)
    abi.findFunction(predicate)
  }

  private def findEvent(abi: Abi, name: String) = {
    val predicate: Predicate[Abi.Event] = (x) => x.name.equals(name)
    abi.findEvent(predicate)
  }

  private def functionId(f: Abi.Function) =
    f.encodeSignature().toString.toUpperCase()

  private def eventId(e: Abi.Event) =
    e.encodeSignature().toString.toUpperCase()

}
