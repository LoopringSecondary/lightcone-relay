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
import scala.concurrent.ExecutionContext
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
  override val isSingleton = true //按照分片id，应当是singleton的

  def getCommon(s: BalanceManagerSettings) =
    base.CommonSettings(Some(s.id), s.roles, s.instances)
}

class BalanceManager()(implicit
  ec: ExecutionContext,
  timeout: Timeout)
  extends Actor {

  var settings: BalanceManagerSettings = null
  def id = settings.id

  trait HandleReqTrait[T, R] {
    def generateUncachedReq(req: T, cachedRes: Option[Any]): Option[T]

    def getFromCache(req: T): Future[Option[Any]] = for {
      res <- Routers.balanceCacher ? req
    } yield Some(res)

    def getUncachedTokens(reqOpt: Option[T]): Future[Option[Any]] = reqOpt match {
      case None => Future.successful(None)
      case Some(req) => for {
        res <- Routers.ethereumAccessor ? req
      } yield Some(res)
    }

    def setUncachedTokenToCache(uncachedRes: Option[Any]): Unit

    def mergeResp(cachedResOpt: Option[Any], uncachedResOpt: Option[Any]): R
  }

  implicit val handleBalanceReq =
    new HandleReqTrait[GetBalancesReq, GetBalancesResp] {

      override def generateUncachedReq(
        req: GetBalancesReq,
        cachedResOpt: Option[Any]): Option[GetBalancesReq] = {
        cachedResOpt match {
          case Some(cachedRes: GetBalancesResp) =>
            var uncachedReqOpt: Option[GetBalancesReq] = None
            val reqTokens = req.tokens.toSet
            val cachedTokens = cachedRes.balances.map(_.token).toSet
            val uncachedTokens = reqTokens -- cachedTokens
            if (uncachedTokens.nonEmpty) {
              uncachedReqOpt = Some(req.withTokens(uncachedTokens.toSeq))
            }
            uncachedReqOpt
          case _ => Some(req)
        }
      }

      override def setUncachedTokenToCache(uncachedResOpt: Option[Any]): Unit = {
        uncachedResOpt match {
          case Some(info: GetBalancesResp) =>
            val cacheBalanceInfo = CacheBalanceInfo(
              address = info.address,
              balances = info.balances)
            Routers.balanceCacher ! cacheBalanceInfo
          case _ => None
        }
      }

      override def mergeResp(
        cachedResOpt: Option[Any],
        uncachedResOpt: Option[Any]): GetBalancesResp = {
        val mergedResp = cachedResOpt match {
          case Some(cachedRes: GetBalancesResp) =>
            uncachedResOpt match {
              case Some(uncachedRes: GetBalancesResp) =>
                GetBalancesResp()
                  .withAddress(cachedRes.address)
                  .withBalances(cachedRes.balances ++ uncachedRes.balances)
              case _ => GetBalancesResp()
            }
          case _ => GetBalancesResp()
        }
        mergedResp
      }

    }

  implicit val handleAllowanceReq =
    new HandleReqTrait[GetAllowancesReq, GetAllowancesResp] {
      override def generateUncachedReq(
        req: GetAllowancesReq,
        cachedResOpt: Option[Any]): Option[GetAllowancesReq] = {
        cachedResOpt match {
          case Some(cachedRes: GetAllowancesResp) =>
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
          case _ => Some(req)
        }
      }

      override def setUncachedTokenToCache(uncachedRes: Option[Any]): Unit = {
        uncachedRes match {
          case Some(info: GetAllowancesResp) =>
            val cacheBalanceInfo = CacheBalanceInfo(
              address = info.address,
              allowances = info.allowances)
            Routers.balanceCacher ! cacheBalanceInfo
          case _ =>
        }
      }

      override def mergeResp(
        cachedResOpt: Option[Any],
        uncachedResOpt: Option[Any]): GetAllowancesResp = {
        cachedResOpt match {
          case Some(cachedRes: GetAllowancesResp) =>
            uncachedResOpt match {
              case Some(resp2: GetAllowancesResp) =>
                GetAllowancesResp()
                  .withAddress(cachedRes.address)
                  .withAllowances(cachedRes.allowances ++ resp2.allowances)
              case _ => cachedRes
            }
          case _ => GetAllowancesResp()
        }
      }
    }

  implicit val handleBalanceAndAllowanceReq =
    new HandleReqTrait[GetBalanceAndAllowanceReq, GetBalanceAndAllowanceResp] {

      override def generateUncachedReq(
        req: GetBalanceAndAllowanceReq,
        cachedResOpt: Option[Any]): Option[GetBalanceAndAllowanceReq] = {
        cachedResOpt match {
          case Some(cachedRes: GetBalanceAndAllowanceResp) =>
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
          case _ => Some(req)
        }

      }

      override def setUncachedTokenToCache(uncachedResOpt: Option[Any]): Unit = {

        uncachedResOpt match {
          case Some(uncachedRes: GetBalanceAndAllowanceResp) =>
            val cacheBalanceInfo = CacheBalanceInfo(
              address = uncachedRes.address,
              balances = uncachedRes.balances,
              allowances = uncachedRes.allowances)
            Routers.balanceCacher ! cacheBalanceInfo
          case _ =>
        }
      }

      override def mergeResp(
        cachedResOpt: Option[Any],
        uncachedResOpt: Option[Any]): GetBalanceAndAllowanceResp = {
        cachedResOpt match {
          case Some(cachedRes: GetBalanceAndAllowanceResp) =>
            uncachedResOpt match {
              case Some(uncachedRes: GetBalanceAndAllowanceResp) =>
                GetBalanceAndAllowanceResp()
                  .withAddress(cachedRes.address)
                  .withAllowances(cachedRes.allowances ++ uncachedRes.allowances)
                  .withBalances(cachedRes.balances ++ uncachedRes.balances)
              case _ => cachedRes
            }
          case _ => GetBalanceAndAllowanceResp() //todo:fix it
        }
      }
    }

  def receive: Receive = {
    case settings: BalanceManagerSettings =>
      this.settings = settings
    case req: GetBalancesReq =>
      val sender = sender()
      handleInfoReq(req, sender)
    case req: GetAllowancesReq =>
      val sender = sender()
      handleInfoReq(req, sender)
    case req: GetBalanceAndAllowanceReq =>
      val sender = sender()
      handleInfoReq(req, sender)
  }

  def handleInfoReq[T, R](req: T, sender: ActorRef)(implicit s: HandleReqTrait[T, R]): Future[Unit] = for {
    cachedRes <- s.getFromCache(req)
    uncachedReq = s.generateUncachedReq(req, cachedRes)
    uncachedRes <- s.getUncachedTokens(uncachedReq)
  } yield {
    s.setUncachedTokenToCache(uncachedRes)
    val mergedResp = s.mergeResp(cachedRes, uncachedRes)
    sender ! mergedResp
  }

}