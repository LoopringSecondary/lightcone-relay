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

package org.loopring.lightcone.core.cache

import com.google.inject._
import com.google.protobuf.ByteString
import org.loopring.lightcone.proto.balance._
import redis._

import scala.concurrent.{ ExecutionContext, Future }

final class BalanceRedisCache @Inject() (
  redis: RedisCluster)(
  implicit
  val ex: ExecutionContext)
  extends BalanceCache {

  val prefix = "balance_"

  override def GetBalances(req: GetBalancesReq): Future[Option[GetBalancesResp]] = for {
    resByteArr <- redis.hmget[Array[Byte]](req.cacheKey, req.cacheField: _*)
  } yield {
    //todo:暂时demo，数据结构确定之后再补充
    Some(GetBalancesResp(
      address = req.address,
      balances = resByteArr.zipWithIndex.map(z =>
        TokenAmount(token = req.tokens(z._2), amount = z._1 match {
          case None => ByteString.EMPTY //todo:
          case Some(value) => ByteString.EMPTY
        }))))
  }

  override def GetAllowances(req: GetAllowancesReq): Future[Option[GetAllowancesResp]] = for {
    resByteArr <- redis.hmget[Array[Byte]](req.cacheKey, req.cacheField: _*)
  } yield {
    //todo:暂时demo，数据结构确定之后再补充
    Some(GetAllowancesResp(
      address = req.address,
      allowances = resByteArr.zipWithIndex.map(z =>
        Allowance(delegate = ByteString.EMPTY, tokenAmounts = Seq(TokenAmount(token = req.tokens(z._2), amount = z._1 match {
          case None => ByteString.EMPTY //todo:
          case Some(value) => ByteString.EMPTY
        }))))))
  }

  override def GetBalanceAndAllowances(req: GetBalancesReq): Future[Option[GetBalanceAndAllowanceResp]] = for {
    resByteArr <- redis.hmget[Array[Byte]](req.cacheKey, req.cacheField: _*)
  } yield {
    //todo:暂时demo，数据结构确定之后再补充
    Some(GetBalanceAndAllowanceResp(
      address = req.address,
      allowances = resByteArr.zipWithIndex.map(z =>
        Allowance(delegate = ByteString.EMPTY, tokenAmounts = Seq(TokenAmount(token = req.tokens(z._2), amount = z._1 match {
          case None => ByteString.EMPTY //todo:
          case Some(value) => ByteString.EMPTY
        }))))))
  }

  implicit class RichGetBalancesReq(req: GetBalancesReq) {
    def cacheKey = prefix + req.address
    def cacheField = req.tokens.map(t => t.toString())
  }

  implicit class RichGetAllowancesReq(req: GetAllowancesReq) {
    def cacheKey = prefix + req.address
    def cacheField = req.tokens.map(t => t.toString())
  }

  implicit class RichGetBalanceAndAllowanceReq(req: GetBalanceAndAllowanceReq) {
    def cacheKey = prefix + req.address
    def cacheField = req.tokens.map(t => t.toString())
  }

}