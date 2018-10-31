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

package org.loopring.lightcone.gateway.api.model

import scala.beans.BeanProperty

class BalanceReq(@BeanProperty var owner: String = "", @BeanProperty var delegateAddress: String = "") extends {
  def this() {
    this("", "")
  }
}

case class TokenBalance(symbol: String, balance: String, allowance: String)
case class BalanceResp(delegateAddress: String, owner: String, tokens: Seq[TokenBalance])