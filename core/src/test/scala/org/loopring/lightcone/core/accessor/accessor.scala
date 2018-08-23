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

package org.loopring.lightcone.core.accessor

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import org.loopring.lightcone.core.converter._

package object accessor {
  implicit val system = ActorSystem()

  val config = GethClientConfig.apply(host = "localhost", port = 8545, ssl = false)
  val geth = new EthClientImpl(config)(system)
  val timeout = Timeout(5 seconds)
  val txconverter = new TransactionConverter()
  val logConverter = new ReceiptLogConverter()
  val receiptconverter = new TransactionReceiptConverter()(logConverter)
  val blockWithTxHashConverter = new BlockWithTxHashConverter()
  val blockWithTxObjectConverter = new BlockWithTxObjectConverter()(txconverter)
  val traceCallConverter = new TraceCallConverter()
  val traceTransactionConverter = new TraceTransactionConverter()(traceCallConverter)
}
