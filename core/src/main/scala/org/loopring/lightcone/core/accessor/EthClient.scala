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
  // def request[R, P](req: R, method: String, params: Seq[Any]): Future[P]

  def ethGetBlockNumber(): Future[EthGetBlockNumberRes]
  def ethGetBalance(req: EthGetBalanceReq): Future[EthGetBalanceRes]
  def getTransactionByHash(req: GetTransactionByHashReq): Future[GetTransactionByHashRes]
  def getTransactionReceipt(req: GetTransactionReceiptReq): Future[GetTransactionReceiptRes]
  def getBlockWithTxHashByNumber(req: GetBlockWithTxHashByNumberReq): Future[GetBlockWithTxHashByNumberRes]
  def getBlockWithTxObjectByNumber(req: GetBlockWithTxObjectByNumberReq): Future[GetBlockWithTxObjectByNumberRes]
  def getBlockWithTxHashByHash(req: GetBlockWithTxHashByHashReq): Future[GetBlockWithTxHashByHashRes]
  def getBlockWithTxObjectByHash(req: GetBlockWithTxObjectByHashReq): Future[GetBlockWithTxObjectByHashRes]

  // todo:数据量太大时报错,需要设置http entity size(akka.http.scaladsl.model.EntityStreamException: HTTP chunk size exceeds the configured limit of 1048576 bytes)
  def traceTransaction(req: TraceTransactionReq): Future[TraceTransactionRes]
  // def getEstimatedGas()

  // erc20
  def getBalance(req: GetBalanceReq): Future[GetBalanceRes]
  def getAllowance(req: GetAllowanceReq): Future[GetAllowanceRes]

  def sendRawTransaction(req: SendRawTransactionReq): Future[SendRawTransactionRes]

}
