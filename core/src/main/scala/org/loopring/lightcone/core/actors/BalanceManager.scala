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
import org.loopring.lightcone.proto.balance.BalanceInfoFromCache.{ CachedInfo, UnCachedInfo }
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
      case UnCachedInfo.GetBalancesReq(uncachedReq) if uncachedReq.tokens.nonEmpty =>
        Routers.ethereumAccessor ? uncachedReq

      case UnCachedInfo.GetAllowancesReq(uncachedReq) if uncachedReq.tokens.nonEmpty =>
        Routers.ethereumAccessor ? uncachedReq

      case UnCachedInfo.GetBalanceAndAllowanceReq(uncachedReq) if uncachedReq.tokens.nonEmpty =>
        Routers.ethereumAccessor ? uncachedReq

      case _ =>
        Future.successful(Unit)
    }

    cachedRes = infoFromCache.cachedInfo match {
      case CachedInfo.GetAllowancesResp(value) => value
      case CachedInfo.GetBalancesResp(value) => value
      case CachedInfo.GetBalanceAndAllowanceResp(value) => value
      case CachedInfo.Empty => Unit
    }

  } yield {
    uncachedRes match {
      case Unit => cachedRes
      case err: ErrorResp => err
      case infoFromEth @ (GetBalancesResp(_, _) | GetAllowancesResp(_, _) | GetBalanceAndAllowanceResp(_, _, _)) =>
        var mergedResp = mergeResp(cachedRes, infoFromEth)
        Routers.balanceCacher ! CacheBalanceInfo
        mergedResp
    }

  }

  def mergeResp(resp1: Any, resp2: Any) = {
    resp1 match {
      case GetBalancesResp => GetBalancesResp()
      case GetAllowancesResp => GetAllowancesResp()
      case GetBalanceAndAllowanceResp => GetBalanceAndAllowanceResp()
    }
  }

}