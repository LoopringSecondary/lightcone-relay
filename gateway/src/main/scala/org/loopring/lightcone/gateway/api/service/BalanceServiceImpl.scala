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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import com.google.inject.Inject
import org.loopring.lightcone.gateway.api.model.{ BalanceReq, BalanceResp, TokenBalance }
import org.loopring.lightcone.gateway.database.DatabaseAccesser

import scala.concurrent.{ ExecutionContext, Future }

class BalanceServiceImpl @Inject() (
    implicit
    session: SlickSession,
    mat: ActorMaterializer
) extends DatabaseAccesser with BalanceService {

  import session.profile.api._

  implicit val toTokenInfo = (r: ResultRow) ⇒
    BalanceResp(delegateAddress = r <<, owner = r <<, tokens = Seq.empty)
  implicit val executeContext = ExecutionContext.global

  override def getBalance(req: BalanceReq): Future[BalanceResp] = {
    println("=" * 50)
    println(req.owner)
    println(req.delegateAddress)

    // for test, will replace by actor next commit
    Future(BalanceResp("1", "2", Seq(TokenBalance("LRC", "0x0001", "0x0001"))))

    //    sql"""select owner, delegateAddress from t_balance where owner<>'1'""".head[BalanceResp]
  }

}
