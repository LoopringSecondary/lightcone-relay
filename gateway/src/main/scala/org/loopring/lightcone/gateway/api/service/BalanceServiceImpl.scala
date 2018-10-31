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

import org.loopring.lightcone.gateway.api.model.{ BalanceReq, BalanceResp }
import org.loopring.lightcone.gateway.socketio.event

import scala.concurrent.{ ExecutionContext, Future }

class BalanceServiceImpl extends BalanceService {

  // broadcat => 0: 不广播直接回复给请求者, 1: 广播订阅者, 2: 主动给广播所有(这种情况event无效)
  @event(event = "aabb", broadcast = 0, interval = -1, replyTo = "ccdd")
  override def getBalance(req: BalanceReq): Future[BalanceResp] = {
    println("xxxxxxxxxxxxxxxxxxx")
    println(req.getOwner)
    println(req.getDelegateAddress)
    //    println(BalanceResp("123", "456", Seq.empty))
    //    Future(BalanceResp("123", "456", Seq.empty))
    // throw new BalanceException("xxxxxalkdfjadjflk")

    Future.successful(BalanceResp("aabbcc", "ddeeff", Seq.empty))
  }
}
