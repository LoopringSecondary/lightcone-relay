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

import org.loopring.lightcone.proto.eth_jsonrpc._
import scala.concurrent._

trait EthClient {
  def request[R, P](req: R, method: String, params: Seq[Any]): Future[P]
  def ethGetBalance(req: EthGetBalanceRequest): Future[EthGetBalanceResponse]
  def getTransactionByHash(req: GetTransactionByHashRequest): Future[GetTransactionByHashResponse]
  def getTransactionReceipt(req: GetTransactionReceiptRequest): Future[GetTransactionReceiptResponse]
  def getBlockWithTxHashByNumber(req: GetBlockWithTxHashByNumberRequest): Future[GetBlockWithTxHashByNumberResponse]
  def getBlockWithTxObjectByNumber(req: GetBlockWithTxObjectByNumberRequest): Future[GetBlockWithTxObjectByNumberResponse]
  def getBlockWithTxHashByHash(req: GetBlockWithTxHashByHashRequest): Future[GetBlockWithTxHashByHashResponse]
  def getBlockWithTxObjectByHash(req: GetBlockWithTxObjectByHashRequest): Future[GetBlockWithTxObjectByHashResponse]
}
