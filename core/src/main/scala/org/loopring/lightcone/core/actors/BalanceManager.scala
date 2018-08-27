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
        var uncachedReqOpt = generateUncachedReq(req.asInstanceOf[GetBalancesReq], cachedRes)
        (Some(cachedRes), uncachedReqOpt)

      case cachedRes: GetAllowancesResp =>
        var uncachedReqOpt = generateUncachedReq(req.asInstanceOf[GetAllowancesReq], cachedRes)
        (Some(cachedRes), uncachedReqOpt)

      case cachedRes: GetBalanceAndAllowanceResp =>
        var uncachedReqOpt = generateUncachedReq(req.asInstanceOf[GetBalanceAndAllowanceReq], cachedRes)
        (Some(cachedRes), uncachedReqOpt)

      case _ => (None, Some(req))
    }

    uncachedResOpt <- uncachedReqOpt match {
      case None => Future.successful(None)
      case Some(uncachedReq) => getFromEthAndCacheRes(uncachedReq)
    }
  } yield {
    val errorFunction: PartialFunction[Any, ErrorResp] = {
      case Some(err: ErrorResp) => err
      case _ => ErrorResp()
    }

    val mergedResp = cachedResOpt match {
      case Some(resp1: GetBalancesResp) =>
        uncachedResOpt match {
          case Some(resp2: GetBalancesResp) =>
            GetBalancesResp()
              .withAddress(resp1.address)
              .withBalances(resp1.balances ++ resp2.balances)
          case _ => errorFunction(_)
        }
      case Some(resp1: GetAllowancesResp) =>
        uncachedResOpt match {
          case Some(resp2: GetAllowancesResp) =>
            GetAllowancesResp()
              .withAddress(resp1.address)
              .withAllowances(resp1.allowances ++ resp2.allowances)
          case _ => errorFunction(_)
        }
      case Some(resp1: GetBalanceAndAllowanceResp) =>
        uncachedResOpt match {
          case Some(resp2: GetBalanceAndAllowanceResp) =>
            GetAllowancesResp()
              .withAddress(resp1.address)
              .withAllowances(resp1.allowances ++ resp2.allowances)
          case _ => errorFunction(_)
        }
      case _ => uncachedResOpt match {
        case Some(resp2: GeneratedMessage) => resp2
        case _ => ErrorResp()
      }
    }
    sender() ! mergedResp
  }

  def getFromEthAndCacheRes(uncachedReq: GeneratedMessage) = for {
    res <- Routers.ethereumAccessor ? uncachedReq
  } yield {
    res match {
      case err: ErrorResp => Some(err)
      case info: GetBalancesResp =>
        val cacheBalanceInfo = CacheBalanceInfo(
          address = info.address,
          balances = info.balances)
        Routers.balanceCacher ! cacheBalanceInfo
        Some(info)
      case info: GetAllowancesResp =>
        val cacheBalanceInfo = CacheBalanceInfo(
          address = info.address,
          allowances = info.allowances)
        Routers.balanceCacher ! cacheBalanceInfo
        Some(info)
      case info: GetBalanceAndAllowanceResp =>
        val cacheBalanceInfo = CacheBalanceInfo(
          address = info.address,
          balances = info.balances,
          allowances = info.allowances)
        Routers.balanceCacher ! cacheBalanceInfo
        Some(info)
      case _ => None
    }
  }

  def generateUncachedReq(req: GetBalancesReq, cachedRes: GetBalancesResp) = {
    var uncachedReqOpt: Option[GetBalancesReq] = None
    val reqTokens = req.tokens.toSet
    val cachedTokens = cachedRes.balances.map(_.token).toSet
    val uncachedTokens = reqTokens -- cachedTokens
    if (uncachedTokens.nonEmpty) {
      uncachedReqOpt = Some(req.withTokens(uncachedTokens.toSeq))
    }
    uncachedReqOpt
  }

  def generateUncachedReq(req: GetAllowancesReq, cachedRes: GetAllowancesResp) = {
    var uncachedReqOpt: Option[GetAllowancesReq] = None
    val r = req.asInstanceOf[GetAllowancesReq]
    val reqTokens = r.tokens.toSet
    val cachedTokens = cachedRes.allowances
      .flatMap(_.tokenAmounts.map(_.token)).toSet
    val uncachedTokens = reqTokens -- cachedTokens
    if (uncachedTokens.nonEmpty) {
      uncachedReqOpt = Some(r.withTokens(uncachedTokens.toSeq))
    }
    uncachedReqOpt
  }

  def generateUncachedReq(req: GetBalanceAndAllowanceReq, cachedRes: GetBalanceAndAllowanceResp) = {
    var uncachedReqOpt: Option[GetBalanceAndAllowanceReq] = None
    val r = req.asInstanceOf[GetBalanceAndAllowanceReq]
    val reqTokens = r.tokens.toSet
    val cachedAllowanceTokens = cachedRes.allowances
      .flatMap(_.tokenAmounts.map(_.token)).toSet
    val cachedBalanceTokens = cachedRes.balances.map(_.token).toSet
    val uncachedAllowanceTokens = reqTokens -- cachedAllowanceTokens
    val uncachedBalanceTokens = reqTokens -- cachedBalanceTokens
    val uncachedTokens = uncachedAllowanceTokens ++ uncachedBalanceTokens
    if (uncachedTokens.nonEmpty) {
      uncachedReqOpt = Some(r.withTokens(uncachedTokens.toSeq))
    }
    uncachedReqOpt
  }

}