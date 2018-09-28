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

package org.loopring.lightcone.core.utils

import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.cache.Purge
import org.loopring.lightcone.proto.order.{ OrderQuery, UpdatedOrder }

import scala.concurrent.Future

trait OrderBookManagerHelper {
  def updateOrder(order: UpdatedOrder)
  def crossingOrdersBetweenPrices(minPrice: Rational, maxPrice: Rational): TokenOrders
  def crossingPrices(canBeMatched: OrderWithStatus ⇒ Boolean): (Rational, Rational) //minPrice, maxPrice
  def resetOrders(query: OrderQuery): Future[Unit]
  def purgeOrders(orderHashes: Seq[String]): Future[Unit]
  def purgeOrders(purge: Purge.AllOrderForAddress): Future[Unit]
  def purgeOrders(purge: Purge.AllForAddresses): Future[Unit]
  def purgeOrders(purge: Purge.All): Future[Unit]
  def purgeOrders(purge: Purge.AllAfterBlock): Future[Unit]
}
