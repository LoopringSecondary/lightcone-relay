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
import org.loopring.lightcone.proto.order.{ MarketSide, Order, OrderType, SoftCancelSign }

class OrderWriteHelperImpl @Inject() (validator: OrderValidator) extends OrderWriteHelper {

  override def generateHash(order: Order): String = ???
  override def fillInOrder(order: Order): Order = {
    val filledRawOrder = order.rawOrder.get.copy(hash = generateHash(order))
    order.copy(rawOrder = Some(filledRawOrder), market = getMarket(order), side = getSide(order).name, price = getPrice(order))
  }

  override def validateOrder(order: Order): ValidateResult = validator.validate(order)

  override def isOrderExist(order: Order): Boolean = ???

  override def getMarket(order: Order): String = ???

  override def getSide(order: Order): MarketSide = ???

  override def getPrice(order: Order): Double = ???

  def validateSoftCancelSign(optSign: Option[SoftCancelSign]): ValidateResult = ???
}
