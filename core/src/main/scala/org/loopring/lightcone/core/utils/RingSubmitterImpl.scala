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

import java.util.concurrent.atomic.AtomicInteger

import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.lib.etypes._
import org.loopring.lightcone.proto.eth_jsonrpc._
import org.web3j.crypto._
import org.web3j.tx.ChainId

import scala.concurrent.Future

class RingSubmitterImpl(
    ethClient: EthClient,
    contract: String = "",
    chainId: Byte = 0.toByte,
    keystorePwd: String = "",
    keystoreFile: String = ""
) extends RingSubmitter {
  val credentials: Credentials = WalletUtils.loadCredentials(keystorePwd, keystoreFile)

  var currentNonce = new AtomicInteger(1) //todo: 启动时,nonce需要初始化, 结合以太坊以及数据库的数据

  override def getSubmitterAddress(): String = credentials.getAddress

  override def signAndSendTx(ring: RingCandidate): Future[SendRawTransactionRes] = {
    val inputData = "" //todo:
    val rawTransaction = RawTransaction.createTransaction(
      BigInt(currentNonce.getAndIncrement()).bigInteger,
      ring.gasPrice.bigInteger,
      ring.gasLimit.bigInteger,
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

}
