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

import akka.actor._
import com.google.inject._
import com.google.inject.name._
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.lib.abi.{ Erc20Abi, LoopringAbi }
import org.spongycastle.util.encoders.Hex
import org.loopring.ethcube.EthereumProxy
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

class EthClientImpl @Inject() (
  @Named("ethereum_proxy") proxy: ActorRef) extends EthClient {

  implicit val timeout = Timeout(3 seconds)

  def ethGetBlockNumber() =
    httpPost[EthGetBlockNumberRes]("eth_blockNumber") {
      Seq()
    }

  def ethGetBalance(req: EthGetBalanceReq) =
    (proxy ? req).mapTo[EthGetBalanceRes]

  def getTransactionByHash(req: GetTransactionByHashReq) =
    (proxy ? req).mapTo[GetTransactionByHashRes]

  def getTransactionReceipt(req: GetTransactionReceiptReq) =
    (proxy ? req).mapTo[GetTransactionReceiptRes]

  def getBlockWithTxHashByNumber(req: GetBlockWithTxHashByNumberReq) =
    (proxy ? req).mapTo[GetBlockWithTxHashByNumberRes]

  def getBlockWithTxObjectByNumber(req: GetBlockWithTxObjectByNumberReq) =
    (proxy ? req).mapTo[GetBlockWithTxObjectByNumberRes]

  def getBlockWithTxHashByHash(req: GetBlockWithTxHashByHashReq) =
    (proxy ? req).mapTo[GetBlockWithTxHashByHashRes]

  def getBlockWithTxObjectByHash(req: GetBlockWithTxObjectByHashReq) =
    (proxy ? req).mapTo[GetBlockWithTxObjectByHashRes]

  def traceTransaction(req: TraceTransactionReq) =
    (proxy ? req).mapTo[TraceTransactionRes]

  def getBalance(req: GetBalanceReq) =
    (proxy ? req).mapTo[GetBalanceRes]

  def getAllowance(req: GetAllowanceReq) =
    (proxy ? req).mapTo[GetAllowanceRes]

  def sendRawTransaction(req: SendRawTransactionReq) =
    (proxy ? req).mapTo[SendRawTransactionRes]

}
