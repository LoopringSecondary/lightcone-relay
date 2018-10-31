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

package org.loopring.lightcone.gateway.api.service

import com.googlecode.jsonrpc4j.{ JsonRpcError, JsonRpcErrors }
import org.loopring.lightcone.gateway.api.model.{ BalanceReq, BalanceResp }
import org.loopring.lightcone.gateway.api.exception.BalanceException

import scala.concurrent.Future

trait BalanceService {
  @JsonRpcErrors(Array(
    new JsonRpcError(exception = classOf[BalanceException], code = -100, message = "User already exists", data = "The Data")
  ))
  def getBalance(req: BalanceReq): Future[BalanceResp]
}