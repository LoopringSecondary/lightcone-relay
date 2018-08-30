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

import java.math.BigInteger

import akka.actor.Actor
import akka.util.Timeout
import org.loopring.lightcone.proto.deployment.RingSubmitterSettings
import org.loopring.lightcone.proto.ring.RingCandidates
import org.web3j.crypto.{ Credentials, RawTransaction, TransactionEncoder, WalletUtils }
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.{ DefaultBlockParameter, Request }
import org.web3j.protocol.core.methods.request.{ EthFilter, ShhFilter, ShhPost, Transaction }
import org.web3j.protocol.core.methods.response
import org.web3j.protocol.core.methods.response._
import org.web3j.tx.ChainId
import rx.Observable

import scala.concurrent.ExecutionContext

object RingSubmitter
  extends base.Deployable[RingSubmitterSettings] {
  val name = "ring_submitter"
  override val isSingleton = true

  def getCommon(s: RingSubmitterSettings) =
    base.CommonSettings(Some(s.id), s.roles, 1)
}

class RingSubmitter()(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends Actor {

  var submitterCredentials = Map[String, Credentials]()
  var chainId = 7107171.toByte
  var contract = ""
  override def receive: Receive = {
    case settings: RingSubmitterSettings =>
      submitterCredentials = settings.keystoreFiles.map { k =>
        val credential = WalletUtils.loadCredentials(k.password, k.file)
        (credential.getAddress, credential)
      }.toMap

    case ringToSettle: RingCandidates =>
      val inputData = ""
      val rawTransaction = RawTransaction.createTransaction(
        BigInt(0).bigInteger,
        BigInt(0).bigInteger,
        BigInt(0).bigInteger,
        contract,
        BigInt(0).bigInteger,
        inputData)
      val signedMessage = signTransaction(rawTransaction, submitterCredentials(""))
  }

  def signTransaction(rawTransaction: RawTransaction, credentials: Credentials) = {
    if (chainId > ChainId.NONE)
      TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
    else
      TransactionEncoder.signMessage(rawTransaction, credentials)
  }
}
