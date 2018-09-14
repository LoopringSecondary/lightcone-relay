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

package org.loopring.lightcone.core.utils

import org.loopring.lightcone.proto.block_chain_event._
import org.loopring.lightcone.proto.eth_jsonrpc._

import scala.concurrent.Future

trait TransactionHelper {

  def getMinedTransactions(hashseq: Seq[String]): Future[Seq[FullTransaction]]
  def getPendingTransactions(hashSeq: Seq[String]): Future[Seq[Transaction]]
  def unpackMinedTransaction(tx: FullTransaction): Seq[Any]
  def unpackPendingTransaction(tx: Transaction): Seq[Any]
  def unpackSingleInput(input: String, header: TxHeader): Seq[Any]
  def unpackSingleEvent(log: Log, header: TxHeader): Seq[Any]
  def isContractAddressSupported(txTo: String): Boolean

}
