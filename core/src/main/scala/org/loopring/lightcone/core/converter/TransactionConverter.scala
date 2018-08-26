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

package org.loopring.lightcone.core.converter

import org.loopring.lightcone.proto.{ eth_jsonrpc => ethj }
import org.loopring.lightcone.proto.eth._
import org.loopring.lightcone.core._

class TransactionConverter
  extends EthDataConverter[ethj.Transaction, Transaction] {

  def convertDown(org: ethj.Transaction) = Transaction()
    .withHash(Hash(org.hash))
    .withNonce(Big(org.nonce))
    .withBlockHash(Hash(org.blockHash))
    .withBlockNumber(Big(org.blockNumber))
    .withTransactionIndex(Big(org.transactionIndex))
    .withFrom(Address(org.from))
    .withTo(Address(org.to))
    .withValue(Big(org.value))
    .withGas(Big(org.gas))
    .withGasPrice(Big(org.gasPrice))
    .withInput(Hex(org.input))
    .withV(Hex(org.v))
    .withR(Hex(org.r))
    .withS(Hex(org.s))
}
