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

import akka.actor._
import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.core.actors.base.RepeatedJobActor
import org.loopring.lightcone.proto.token._
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.lib.solidity.Abi

import scala.concurrent.Future

object BlockchainEventExtractor
  extends base.Deployable[BlockchainEventExtractorSettings] {
  val name = "block_event_extractor"
  val isSingleton = true

  def props = Props(classOf[BlockchainEventExtractor])

  def getCommon(s: BlockchainEventExtractorSettings) =
    base.CommonSettings("", s.roles, 1)
}

class BlockchainEventExtractor(
  val tokenList: Seq[Token],
  val abiStrMap: Map[String, String])(implicit val accessor: EthClient) extends RepeatedJobActor {

  val (abiFunctions: Map[String, Abi.Function], abiEvents: Map[String, Abi.Event]) = {
    var fmap: Map[String, Abi.Function] = Map()
    var emap: Map[String, Abi.Event] = Map()

    val abimap = abiStrMap.map(x => { x._1.toLowerCase() -> Abi.fromJson(x._2) })

    abimap.map(x => x._2.iterator.next() match {
      case f: Abi.Function => fmap += f.encodeSignature().toString.toUpperCase() -> f
      case e: Abi.Event => emap += e.encodeSignature().toString.toUpperCase() -> e
    })

    (fmap, emap)
  }

  // todo: get protocol address(delegate, impl, token register...) on chain
  val supportedContracts: Seq[String] = tokenList.map(x => safeAddress(x.protocol))

  // todo: 首次从数据库获取blockNumber,后续启动从数据库获取blockNumber
  val currentBlock: BigInt = ???

  override def receive: Receive = {
    case settings: BlockchainEventExtractorSettings =>
  }

  // get block transactions receipts
  // fork detecting
  // decode methods and events
  // send data
  override def handleRepeatedJob(): Future[Unit] = ???

  // todo
  def handleSingleTransaction() = ???

  // todo: decode input or event data and convert to onchain event
  def unpack(): AnyRef = ???

  def isSupportedFunction(txTo: String, txInput: String): Boolean = {
    require(isProtocolSupported(txTo))
    val functionId = txInput.substring(0, 4).toUpperCase()
    abiFunctions.contains(functionId)
  }

  def isSupportedEvent(txTo: String, firstTopic: String): Boolean = {
    require(isProtocolSupported(txTo))
    val eventId = getEventId(firstTopic)
    abiEvents.contains(eventId)
  }

  private def isProtocolSupported(txTo: String): Boolean = supportedContracts.contains(safeAddress(txTo))

  private def getFunctionId(txInput: String): String = txInput.substring(0, 4).toUpperCase
  private def getEventId(firstTopic: String): String = firstTopic.toUpperCase()

  // todo: other validate
  private def safeAddress(address: String): String = address.toUpperCase()
}
