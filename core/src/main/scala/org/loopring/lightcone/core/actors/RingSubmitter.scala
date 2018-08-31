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

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.Actor
import akka.util.Timeout
import org.loopring.lightcone.core.accessor.EthClient
import org.loopring.lightcone.core.etypes._
import org.loopring.lightcone.proto.deployment.RingSubmitterSettings
import org.loopring.lightcone.proto.eth_jsonrpc.SendRawTransactionReq
import org.loopring.lightcone.proto.ring.RingToSettleSeq
import org.web3j.crypto.{ Credentials, RawTransaction, TransactionEncoder, WalletUtils }
import org.web3j.tx.ChainId

import scala.concurrent.ExecutionContext

object RingSubmitter
  extends base.Deployable[RingSubmitterSettings] {
  val name = "ring_submitter"
  override val isSingleton = true

  def getCommon(s: RingSubmitterSettings) =
    base.CommonSettings(Some(s.id), s.roles, 1)
}

case class Submitter(nonce: AtomicInteger, credentials: Credentials)

class RingSubmitter(ethClient: EthClient)(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends Actor {

  var submitterCredentials = Map[String, Submitter]()
  var chainId = 1.toByte

  var contract = ""

  override def receive: Receive = {
    case settings: RingSubmitterSettings =>
      submitterCredentials = settings.keystoreFiles.map { k =>
        val credential = WalletUtils.loadCredentials(k.password, k.file)
        val currentNonce = 1 //todo: 启动时,nonce需要初始化, 结合以太坊以及数据库的数据
        (credential.getAddress, Submitter(new AtomicInteger, credential))
      }.toMap
      contract = settings.contract
      chainId = settings.chainId.byteValue()

    case ringSeq: RingToSettleSeq =>
      ringSeq.rings map { ring =>
        val inputData = "" //todo:
        val submitter = submitterCredentials(ring.submitter)
        val nonce = submitter.nonce.getAndIncrement()
        val rawTransaction = RawTransaction.createTransaction(
          BigInt(nonce).bigInteger,
          ring.gasPrice.asBigInteger,
          ring.gasPimit.asBigInteger,
          contract,
          BigInt(0).bigInteger,
          inputData)
        val signedMessage = signTransaction(rawTransaction, submitter.credentials)

        ethClient.sendRawTransaction(SendRawTransactionReq(data = BigInt(signedMessage).toHex))
      }
  }

  def signTransaction(rawTransaction: RawTransaction, credentials: Credentials) = {
    if (chainId > ChainId.NONE)
      TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
    else
      TransactionEncoder.signMessage(rawTransaction, credentials)
  }
}
