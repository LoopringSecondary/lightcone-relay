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
import akka.pattern.ask
import akka.util.Timeout
import org.loopring.lightcone.core.routing.Routers
import org.loopring.lightcone.proto.balance._
import org.loopring.lightcone.proto.cache.CacheBalanceInfo
import org.loopring.lightcone.proto.common.ErrorResp
import org.loopring.lightcone.proto.deployment._
import scalapb.GeneratedMessage

import scala.concurrent.Future

object BalanceManager
  extends base.Deployable[BalanceManagerSettings] {
  val name = "balance_manager"
  val isSingleton = true //按照分片id，应当是singleton的

  def props = Props(classOf[BalanceManager])
  def getCommon(s: BalanceManagerSettings) =
    base.CommonSettings(s.id, s.roles, s.instances)
}

class BalanceManager()(implicit timeout: Timeout) extends Actor {
  import context.dispatcher
  var id = ""

  def receive: Receive = {
    case settings: BalanceManagerSettings =>
      id = settings.id

    case req: GetBalancesReq => handleInfoReq(req)
    case req: GetAllowancesReq => handleInfoReq(req)
    case req: GetBalanceAndAllowanceReq => handleInfoReq(req)

  }

  def handleInfoReq(req: GeneratedMessage) = for {
    cachedInfoRes <- Routers.balanceCacher ? req

    (cachedResOpt, uncachedReqOpt) = cachedInfoRes match {
      case cachedRes: GetBalancesResp =>
        var uncachedReqOpt: Option[GetBalancesReq] = None
        val r = req.asInstanceOf[GetBalancesReq]
        val cachedTokens = cachedRes.balances.map(_.token).toSet
        val reqTokens = r.tokens.toSet
        val uncachedTokens = reqTokens -- cachedTokens
        if (uncachedTokens.nonEmpty) {
          uncachedReqOpt = Some(r.withTokens(uncachedTokens.toSeq))
        }
        (Some(cachedRes), uncachedReqOpt)

      case cachedRes: GetAllowancesResp =>
        var uncachedReqOpt: Option[GetAllowancesReq] = None
        val r = req.asInstanceOf[GetAllowancesReq]
        val cachedTokens = cachedRes.allowances.flatMap(_.tokenAmounts.map(_.token)).toSet
        val reqTokens = r.tokens.toSet
        val uncachedTokens = reqTokens -- cachedTokens
        if (uncachedTokens.nonEmpty) {
          uncachedReqOpt = Some(r.withTokens(uncachedTokens.toSeq))
        }
        (Some(cachedRes), uncachedReqOpt)

      case cachedRes: GetBalanceAndAllowanceResp =>
        var uncachedReqOpt: Option[GetBalanceAndAllowanceReq] = None
        val r = req.asInstanceOf[GetBalanceAndAllowanceReq]
        val cachedAllowanceTokens = cachedRes.allowances.flatMap(_.tokenAmounts.map(_.token)).toSet
        val cachedBalanceTokens = cachedRes.balances.map(_.token).toSet
        val reqTokens = r.tokens.toSet
        val uncachedAllowanceTokens = reqTokens -- cachedAllowanceTokens
        val uncachedBalanceTokens = reqTokens -- cachedBalanceTokens
        val uncachedTokens = uncachedAllowanceTokens ++ uncachedBalanceTokens
        if (uncachedTokens.nonEmpty) {
          uncachedReqOpt = Some(r.withTokens(uncachedTokens.toSeq))
        }
        (Some(cachedRes), uncachedReqOpt)

      case e: ErrorResp => (None, Some(req))

      case _ => (None, Some(req))
    }

    uncachedResOpt <- uncachedReqOpt match {
      case None => Future.successful(None)
      case Some(uncachedReq) => for {
        res <- Routers.ethereumAccessor ? uncachedReq
      } yield {
        res match {
          case info: GeneratedMessage =>
            Routers.balanceCacher ! CacheBalanceInfo
            Some(info)
          case _ => None
        }
      }
    }
  } yield {
    val mergedResp = mergeResp(cachedResOpt, uncachedResOpt)
    sender() ! mergedResp
  }

  def mergeResp(cachedRes: Option[GeneratedMessage], uncachedRes: Option[GeneratedMessage]): Option[GeneratedMessage] =
    (cachedRes, uncachedRes) match {
      case (None, None) => None
      case (Some(res1: GetBalancesResp), Some(res2: GetBalancesResp)) =>
        val resp = GetBalancesResp()
            .withAddress(res1.address)
            .withBalances(res1.balances ++ res2.balances)
        Some(resp)

      case (Some(res1: GetAllowancesResp), Some(res2: GetAllowancesResp)) =>
        val resp = GetAllowancesResp()
            .withAddress(res1.address)
            .withAllowances(res1.allowances ++ res2.allowances)
        Some(resp)

      case (Some(res1: GetBalanceAndAllowanceResp), Some(res2: GetBalanceAndAllowanceResp)) =>
        val resp = GetBalanceAndAllowanceResp()
            .withAddress(res1.address)
            .withAllowances(res1.allowances ++ res2.allowances)
            .withBalances(res1.balances ++ res2.balances)
        Some(resp)

      case (_, Some(errorResp: ErrorResp)) => Some(errorResp)
      case _ => None
    }
}