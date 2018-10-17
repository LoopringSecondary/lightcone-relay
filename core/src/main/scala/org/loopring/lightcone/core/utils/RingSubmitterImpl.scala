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

package org.loopring.lightcone.core.utils

import java.util.concurrent.atomic.AtomicInteger

import akka.util.Timeout
import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.core.database.OrderDatabase
import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.loopring.lightcone.proto.ring.Rings
import org.web3j.crypto._
import org.web3j.tx.ChainId

import scala.concurrent.{ ExecutionContext, Future }

class RingSubmitterImpl(
    ethClient: EthClient,
    contract: String = "",
    chainId: Byte = 0.toByte,
    module: OrderDatabase,
    keystorePwd: String = "",
    keystoreFile: String = ""
)(
    implicit
    ec: ExecutionContext, timeout: Timeout
) extends RingSubmitter {
  val credentials: Credentials = WalletUtils.loadCredentials(keystorePwd, keystoreFile)

  var currentNonce = new AtomicInteger(1) //todo: 启动时,nonce需要初始化, 结合以太坊以及数据库的数据

  override def getSubmitterAddress(): String = credentials.getAddress

  override def signAndSendTx(r: Rings): Future[SendRawTransactionRes] = {
    val inputData = "" //todo:
    val rawTransaction = RawTransaction.createTransaction(
      BigInt(currentNonce.getAndIncrement()).bigInteger,
      r.gasPrice.asBigInteger,
      r.gasLimit.asBigInteger,
      contract,
      BigInt(0).bigInteger,
      inputData
    )
    val signedMessage = signTx(rawTransaction, credentials)
    ethClient.sendRawTransaction(SendRawTransactionReq(data = BigInt(signedMessage).toHex))
  }

  def signTx(rawTransaction: RawTransaction, credentials: Credentials): Array[Byte] = {
    if (chainId > ChainId.NONE)
      TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
    else
      TransactionEncoder.signMessage(rawTransaction, credentials)
  }

  //重新提交未打块的环路
  def resubmitUnblockedRings(untilTime: Long) = for {
    rings ← module.rings.getUnblockedRings(untilTime)
  } yield {
    rings map this.signAndSendTx
  }
}
