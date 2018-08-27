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
import org.loopring.lightcone.proto.balance.BalanceInfoFromCache._
import org.loopring.lightcone.proto.balance._
import org.loopring.lightcone.proto.cache.CacheBalanceInfo
import org.loopring.lightcone.proto.common.ErrorResp
import org.loopring.lightcone.proto.deployment._

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

    case req @ (GetBalancesReq
      | GetAllowancesReq
      | GetBalanceAndAllowanceReq
      ) =>
      for {
        cachedInfoRes <- Routers.balanceCacher ? req
        info <- cachedInfoRes match {
          case m: BalanceInfoFromCache => processInfoFromCache(m)
          case e: ErrorResp => for {
            res <- Routers.ethereumAccessor ? req
          } yield {
            res match {
              case e: ErrorResp => e
              case info @ (GetBalancesResp
                | GetAllowancesResp
                | GetBalanceAndAllowanceResp
                ) =>
                Routers.balanceCacher ! CacheBalanceInfo
                info
            }
          }
        }
      } yield info

  }

  def processInfoFromCache(infoFromCache: BalanceInfoFromCache) = for {
    uncachedRes <- infoFromCache.unCachedInfo match {
      case UnCachedInfo
        .GetBalancesReq(uncachedReq) if uncachedReq.tokens.nonEmpty =>
        for {
          res1 <- Routers.ethereumAccessor ? uncachedReq
        } yield Some(res1)

      case UnCachedInfo
        .GetAllowancesReq(uncachedReq) if uncachedReq.tokens.nonEmpty =>
        for {
          res1 <- Routers.ethereumAccessor ? uncachedReq
        } yield Some(res1)

      case UnCachedInfo
        .GetBalanceAndAllowanceReq(uncachedReq) if uncachedReq.tokens.nonEmpty =>
        for {
          res1 <- Routers.ethereumAccessor ? uncachedReq
        } yield Some(res1)

      case _ => Future.successful(None)
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
      case infoFromEth @ Some(GetBalancesResp
        | GetAllowancesResp
        | GetBalanceAndAllowanceResp
        ) =>
        Routers.balanceCacher ! CacheBalanceInfo
      case _ =>
    }
    mergedResp
  }

  def mergeResp(cachedRes: Any, uncachedRes: Any) =
    (cachedRes, uncachedRes) match {
      case (None, None) =>
      case (Some(res1: GetBalancesResp), Some(res2: GetBalancesResp)) => GetBalancesResp()
      case (Some(res1: GetAllowancesResp), Some(res2: GetAllowancesResp)) => GetAllowancesResp()
      case (Some(res1: GetBalanceAndAllowanceResp), Some(res2: GetBalanceAndAllowanceResp)) => GetBalanceAndAllowanceResp()
      case (_, Some(errorResp: ErrorResp)) => errorResp
    }
}