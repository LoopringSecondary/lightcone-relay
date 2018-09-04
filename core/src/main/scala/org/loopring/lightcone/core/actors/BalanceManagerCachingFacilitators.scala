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
import org.loopring.lightcone.proto.cache.CacheBalanceInfo

object BalanceManagerCachingFacilitators {

  implicit val getBalancesReqFacilitator = new ActorCachingFacilitator[GetBalancesReq, GetBalancesResp, CacheBalanceInfo] {
    def genCacheRequest(req: GetBalancesReq, uncachedResp: GetBalancesResp): Option[CacheBalanceInfo] = {
      Some(CacheBalanceInfo(
        address = uncachedResp.address,
        balances = uncachedResp.balances))
    }

    def genSourceRequest(req: GetBalancesReq, cachedResp: GetBalancesResp): Option[GetBalancesReq] = {
      var uncachedReqOpt: Option[GetBalancesReq] = None
      val reqTokens = req.tokens.toSet
      val cachedTokens = cachedResp.balances.map(_.token).toSet
      val uncachedTokens = reqTokens -- cachedTokens
      if (uncachedTokens.nonEmpty) {
        uncachedReqOpt = Some(req.withTokens(uncachedTokens.toSeq))
      }
      uncachedReqOpt
    }

    def mergeResponses(req: GetBalancesReq, cachedResp: GetBalancesResp, uncachedResp: GetBalancesResp): GetBalancesResp = {
      val mergedResp = GetBalancesResp()
        .withAddress(cachedResp.address)
        .withBalances(cachedResp.balances ++ uncachedResp.balances)
      mergedResp
    }
  }

  implicit val getAllowancesReqFacilitator = new ActorCachingFacilitator[GetAllowancesReq, GetAllowancesResp, CacheBalanceInfo] {
    def genCacheRequest(req: GetAllowancesReq, uncachedResp: GetAllowancesResp): Option[CacheBalanceInfo] = {
      Some(CacheBalanceInfo(
        address = uncachedResp.address,
        allowances = uncachedResp.allowances))
    }

    def genSourceRequest(req: GetAllowancesReq, cachedResp: GetAllowancesResp): Option[GetAllowancesReq] = {
      var uncachedReqOpt: Option[GetAllowancesReq] = None
      val r = req.asInstanceOf[GetAllowancesReq]
      val reqTokens = r.tokens.toSet
      val cachedTokens = cachedResp.allowances
        .flatMap(_.tokenAmounts.map(_.token)).toSet
      val uncachedTokens = reqTokens -- cachedTokens
      if (uncachedTokens.nonEmpty) {
        uncachedReqOpt = Some(r.withTokens(uncachedTokens.toSeq))
      }
      uncachedReqOpt
    }

    def mergeResponses(req: GetAllowancesReq, cachedResp: GetAllowancesResp, uncachedResp: GetAllowancesResp): GetAllowancesResp = {
      GetAllowancesResp()
        .withAddress(cachedResp.address)
        .withAllowances(cachedResp.allowances ++ uncachedResp.allowances)
    }
  }

  implicit val getBalanceAndAllowanceReqFacilitator = new ActorCachingFacilitator[GetBalanceAndAllowanceReq, GetBalanceAndAllowanceResp, CacheBalanceInfo] {
    def genCacheRequest(req: GetBalanceAndAllowanceReq, uncachedResp: GetBalanceAndAllowanceResp): Option[CacheBalanceInfo] = {
      Some(CacheBalanceInfo(
        address = uncachedResp.address,
        balances = uncachedResp.balances,
        allowances = uncachedResp.allowances))
    }

    def genSourceRequest(req: GetBalanceAndAllowanceReq, cachedResp: GetBalanceAndAllowanceResp): Option[GetBalanceAndAllowanceReq] = {
      var uncachedReqOpt: Option[GetBalanceAndAllowanceReq] = None
      val r = req.asInstanceOf[GetBalanceAndAllowanceReq]
      val reqTokens = r.tokens.toSet
      val cachedAllowanceTokens = cachedResp.allowances
        .flatMap(_.tokenAmounts.map(_.token)).toSet
      val cachedBalanceTokens = cachedResp.balances.map(_.token).toSet
      val uncachedAllowanceTokens = reqTokens -- cachedAllowanceTokens
      val uncachedBalanceTokens = reqTokens -- cachedBalanceTokens
      val uncachedTokens = uncachedAllowanceTokens ++ uncachedBalanceTokens
      if (uncachedTokens.nonEmpty) {
        uncachedReqOpt = Some(r.withTokens(uncachedTokens.toSeq))
      }
      uncachedReqOpt
    }

    def mergeResponses(req: GetBalanceAndAllowanceReq, cachedResp: GetBalanceAndAllowanceResp, uncachedResp: GetBalanceAndAllowanceResp): GetBalanceAndAllowanceResp = {
      GetBalanceAndAllowanceResp()
        .withAddress(cachedResp.address)
        .withAllowances(cachedResp.allowances ++ uncachedResp.allowances)
        .withBalances(cachedResp.balances ++ uncachedResp.balances)
    }
  }

}