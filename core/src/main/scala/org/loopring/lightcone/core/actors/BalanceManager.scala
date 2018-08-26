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
import org.loopring.lightcone.proto.balance.BalanceInfoFromCache.{CachedInfo, UnCachedInfo}
import org.loopring.lightcone.proto.balance._
import org.loopring.lightcone.proto.cache.CacheBalanceInfo
import org.loopring.lightcone.proto.common.ErrorResp
import org.loopring.lightcone.proto.deployment._

import scala.concurrent.Future

object BalanceManager
  extends base.Deployable[BalanceManagerSettings] {
  val name = "balance_manager"
  val isSingleton = false

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

    case req @ (GetBalancesReq(_, _) | GetAllowancesReq(_, _, _) | GetBalanceAndAllowanceReq(_, _, _)) => for {
      cachedInfoRes <- Routers.balanceCacher ? req
      info <- cachedInfoRes match {
        case err: ErrorResp => for {
          ethRes <- Routers.ethereumAccessor ? req
        } yield {
          ethRes match {
            case err: ErrorResp => err
            case info @ (GetBalancesResp(_, _) | GetAllowancesResp(_, _) | GetBalanceAndAllowanceResp(_, _, _)) =>
              Routers.balanceCacher ! CacheBalanceInfo
              info
          }
        }
        case infoFromCache: BalanceInfoFromCache => processInfoFromCache(infoFromCache)
      }
    } yield info

  }

  def processInfoFromCache(infoFromCache: BalanceInfoFromCache) = for {
    uncachedRes <- infoFromCache.unCachedInfo match {
      case UnCachedInfo.GetBalancesReq(uncachedReq) if uncachedReq.tokens.nonEmpty => for {
        res1 <- Routers.ethereumAccessor ? uncachedReq
      } yield Some(res1)

      case UnCachedInfo.GetAllowancesReq(uncachedReq) if uncachedReq.tokens.nonEmpty => for {
        res1 <- Routers.ethereumAccessor ? uncachedReq
      } yield Some(res1)

      case UnCachedInfo.GetBalanceAndAllowanceReq(uncachedReq) if uncachedReq.tokens.nonEmpty => for {
        res1 <- Routers.ethereumAccessor ? uncachedReq
      } yield Some(res1)

      case _ =>
        Future.successful(None)
    }

    cachedRes = infoFromCache.cachedInfo match {
      case CachedInfo.GetAllowancesResp(value) => Some(value)
      case CachedInfo.GetBalancesResp(value) => Some(value)
      case CachedInfo.GetBalanceAndAllowanceResp(value) => Some(value)
      case CachedInfo.Empty => None
    }
  } yield {
    var mergedResp = mergeResp(cachedRes, uncachedRes)
    uncachedRes match {
      case infoFromEth @ Some(GetBalancesResp(_, _) | GetAllowancesResp(_, _) | GetBalanceAndAllowanceResp(_, _, _)) =>
        Routers.balanceCacher ! CacheBalanceInfo
      case _ =>
    }
    mergedResp
  }

  def mergeResp(cachedRes: Any, uncachedRes: Any) = (cachedRes, uncachedRes) match {
    case (None, None) =>
    case (res1: GetBalancesResp, res2: GetBalancesResp) => GetBalancesResp()
    case (res1: GetAllowancesResp, res2: GetAllowancesResp) => GetAllowancesResp()
    case (res1: GetBalanceAndAllowanceResp, res2: GetBalanceAndAllowanceResp) => GetBalanceAndAllowanceResp()
    case _ =>
  }
}