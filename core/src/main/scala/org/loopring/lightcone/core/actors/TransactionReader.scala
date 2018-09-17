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
import akka.util.Timeout
import org.loopring.lightcone.proto.deployment.TransactionReaderSettings

import scala.concurrent.ExecutionContext

object TransactionReader
  extends base.Deployable[TransactionReaderSettings] {
  val name = "transaction_manager"

  def getCommon(s: TransactionReaderSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class TransactionReader()(implicit
    ec: ExecutionContext,
    timeout: Timeout
) extends Actor {

  def receive: Receive = {
    case settings: TransactionReaderSettings ⇒
    case _ ⇒
  }
}
