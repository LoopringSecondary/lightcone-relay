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

package org.loopring.lightcone.core.order

import com.google.inject.Inject
import org.loopring.lightcone.core.database.dals.QueryCondition
import org.loopring.lightcone.core.database.tables.Orders
import org.loopring.lightcone.core.database.{ OrderDatabase, tables }
import org.loopring.lightcone.proto.common.{ Pagination, PaginationQuery }
import org.loopring.lightcone.proto.order._
import slick.jdbc.JdbcProfile
import slick.sql.FixedSqlAction

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success }
import scala.concurrent.duration._

class OrderAccessHelperImpl @Inject() (val module: OrderDatabase) extends OrderAccessHelper {

  implicit val profile: JdbcProfile = module.profile
  implicit val executor: ExecutionContext = module.dbec
  import profile.api._
  val defaultSkip = 0
  val defaultTake = 10

  override def saveOrder(order: Order): Future[OrderSaveResult] = {

    if (order.rawOrder.isEmpty) {
      return Future(OrderSaveResult.SUBMIT_FAILED)
    }

    val getOrderRst = getOrderByHash(order.rawOrder.get.hash)
    val optOrder = Await.result(getOrderRst, 1 seconds)
    if (optOrder.isDefined) {
      return Future(OrderSaveResult.ORDER_EXIST)
    }

    val changeLog = OrderChangeLog()

    val a = (for {
      saveOrderRst ← tables.ordersQ += order
      saveLogRst ← tables.orderChangeLogsQ += changeLog
    } yield (saveOrderRst, saveLogRst)).transactionally

    val withErrorHandling = a.asTry.flatMap {
      case Failure(e: Throwable) ⇒
        // print log info
        // print(e)
        // DBIO.failed(e)
        DBIO.successful(OrderSaveResult.SUBMIT_FAILED)
      case Success(_) ⇒ DBIO.successful(OrderSaveResult.SUBMIT_SUCC)
    }
    module.db.run(withErrorHandling)
  }

  override def getOrderByHash(orderHash: String): Future[Option[Order]] = module.orders.getOrder(orderHash)

  override def pageQueryOrders(optOrderQuery: Option[OrderQuery], optPage: Option[PaginationQuery]): Future[MultiOrders] = {
    val (skip, take) = wrapToSkipAndTake(optPage)
    val queryCondition = wrapToQueryCondition(optOrderQuery)
    for {
      fOrders ← module.orders.getOrders(queryCondition, skip, take)
      fPage ← module.orders.count(queryCondition)
    } yield MultiOrders(fOrders, Some(Pagination(skip + 1, take, fPage)))
  }

  //  def softCancel(cancelOption: CancelOrderOption) : Future[Seq[Order]] = {
  //    val willCancelOrders = getSoftCancelOrders(Some(cancelOption))
  //    val orders = Await.result(willCancelOrders, 1 second)
  //    if (orders.isEmpty) {
  //      Future(Seq[Order]())
  //    } else {
  //
  //    }
  //  }

  override def softCancelOrders(cancelOrderOption: Option[CancelOrderOption]): Future[Seq[Order]] = {

    // 软取消的数据库操作，分为如下几个Action:
    // 1. 根据软取消条件，获取所有待取消的order，如果数量是0，返回结果：没有订单可以取消
    // 2. 根据软取消条件，取消所有订单，返回的update数量如果和第1步数量不一致，回滚交易，返回取消失败错误
    // 3. 第2步成功的情况下，批量插入changeLog记录，如果失败，回滚交易，返回取消失败错误
    // 4. 以上步骤都成功的情况下，返回取消成功和第1步的订单列表

    val ordersToCancelAction = getSoftCancelOrdersAction(cancelOrderOption)
    val cancelOrderAction = getSoftCancelAction(cancelOrderOption.get)

    val a = (for {
      orders ← ordersToCancelAction.get.result
      cancelledCount ← cancelOrderAction if orders.size == cancelledCount
      _ ← module.orderChangeLogs.query ++= orders.map(o ⇒ buildChangeLog(o)) if orders.size == cancelledCount
    } yield (orders, cancelledCount)).transactionally
    module.db.run(a).map(o ⇒ {
      if (o._1.size != o._2)
        Seq[Order]()
      else o._1
    })
  }

  def getSoftCancelOrdersAction(cancelOption: Option[CancelOrderOption]): Option[Query[Orders, Order, Seq]] = {
    val qc = cancelOption match {
      case Some(condition) ⇒ condition.cancelType match {
        case SoftCancelType.BY_OWNER ⇒
          Some(QueryCondition(
            owner = Some(condition.owner),
            status = Seq(OrderLevel1Status.ORDER_STATUS_NEW.name)
          ))
        case SoftCancelType.BY_ORDER_HASH ⇒
          Some(QueryCondition(
            orderHashes = Seq(condition.orderHash),
            status = Seq(OrderLevel1Status.ORDER_STATUS_NEW.name)
          ))
        case SoftCancelType.BY_TIME ⇒
          Some(QueryCondition(
            orderHashes = Seq(condition.orderHash)
          ))
        case SoftCancelType.BY_MARKET ⇒
          Some(QueryCondition(
            owner = Some(condition.owner),
            market = Some(condition.market),
            status = Seq(OrderLevel1Status.ORDER_STATUS_NEW.name)
          ))
        case SoftCancelType.Unrecognized(_) ⇒ None

      }
      case None ⇒ None
    }
    if (qc.isEmpty) {
      None
    } else {
      Some(module.orders.unwrapCondition(qc.get))
    }
  }

  def getSoftCancelAction(condition: CancelOrderOption): FixedSqlAction[Int, NoStream, slick.dbio.Effect.Write] = condition.cancelType match {
    case SoftCancelType.BY_OWNER ⇒
      module.orders.softCancelByOwner(condition.owner)
    case SoftCancelType.BY_ORDER_HASH ⇒
      module.orders.softCancelByHash(condition.orderHash)
    case SoftCancelType.BY_TIME ⇒
      module.orders.softCancelByTime(condition.owner, condition.cutOffTime)
    case SoftCancelType.BY_MARKET ⇒
      module.orders.softCancelByHash(condition.orderHash)
  }

  private def buildChangeLog(order: Order): OrderChangeLog = {
    OrderChangeLog(
      orderHash = order.rawOrder.get.hash,
      // no need for now
      // preChangeId = 0L,
      dealtAmountS = order.dealtAmountS,
      dealtAmountB = order.dealtAmountB,
      splitAmountS = order.splitAmountS,
      splitAmountB = order.splitAmountB,
      cancelledAmountS = order.cancelledAmountS,
      cancelledAmountB = order.cancelledAmountB,
      status = order.status.get.level1Status.name,
      updatedBlock = order.updatedBlock,
      createdAt = System.currentTimeMillis / 1000,
      updatedAt = System.currentTimeMillis / 1000
    )
  }

  private def wrapToQueryCondition(optOrderQuery: Option[OrderQuery]): QueryCondition = optOrderQuery match {
    case None ⇒ QueryCondition()
    case Some(query) ⇒ QueryCondition(
      delegateAddress = query.delegate,
      owner = if (query.owner.isEmpty) None else { Some(query.owner) },
      market = if (query.market.isEmpty) None else { Some(query.market) },
      status = query.status,
      orderHashes = query.orderHashes,
      orderType = if (query.orderType.isEmpty) None else { Some(query.orderType) },
      side = if (query.side.isEmpty) None else { Some(query.side) }
    )
  }

  private def wrapToSkipAndTake(optPageInfo: Option[PaginationQuery]): (Int, Int) = optPageInfo match {
    case None ⇒ (defaultSkip, defaultTake)
    case Some(pi) ⇒
      var skip = defaultSkip
      var take = defaultTake
      if (pi.size > 0 && pi.size != defaultTake)
        take = pi.size
      if (pi.index > 1)
        skip = (pi.index - 1) * take
      (skip, take)
  }
}
