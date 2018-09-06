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
import org.loopring.lightcone.proto.balance._
import org.loopring.lightcone.core.cache.actorcache._

object BalanceManagerCachingFacilitators {
  implicit val getBalancesReqFacilitator = new ActorCachingFacilitator[GetBalancesReq, GetBalancesResp, GetBalancesResp] {
    def genCacheRequest(req: GetBalancesReq, uncachedResp: GetBalancesResp): Option[GetBalancesResp] = ???
    def genSourceRequest(req: GetBalancesReq, cachedResp: GetBalancesResp): Option[GetBalancesReq] = ???
    def mergeResponses(req: GetBalancesReq, cachedResp: GetBalancesResp, uncachedResp: GetBalancesResp): GetBalancesResp = ???
  }

  implicit val getAllowancesReqFacilitator = new ActorCachingFacilitator[GetAllowancesReq, GetAllowancesResp, GetAllowancesResp] {
    def genCacheRequest(req: GetAllowancesReq, uncachedResp: GetAllowancesResp): Option[GetAllowancesResp] = ???
    def genSourceRequest(req: GetAllowancesReq, cachedResp: GetAllowancesResp): Option[GetAllowancesReq] = ???
    def mergeResponses(req: GetAllowancesReq, cachedResp: GetAllowancesResp, uncachedResp: GetAllowancesResp): GetAllowancesResp = ???
  }

  implicit val getBalanceAndAllowanceReqFacilitator = new ActorCachingFacilitator[GetBalanceAndAllowanceReq, GetBalanceAndAllowanceResp, GetBalanceAndAllowanceResp] {
    def genCacheRequest(req: GetBalanceAndAllowanceReq, uncachedResp: GetBalanceAndAllowanceResp): Option[GetBalanceAndAllowanceResp] = ???
    def genSourceRequest(req: GetBalanceAndAllowanceReq, cachedResp: GetBalanceAndAllowanceResp): Option[GetBalanceAndAllowanceReq] = ???
    def mergeResponses(req: GetBalanceAndAllowanceReq, cachedResp: GetBalanceAndAllowanceResp, uncachedResp: GetBalanceAndAllowanceResp): GetBalanceAndAllowanceResp = ???
  }

}