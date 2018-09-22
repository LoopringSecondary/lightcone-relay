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

import org.loopring.lightcone.proto.order.{ Order, SoftCancelSign }

trait OrderWriteHelper {
  def generateHash(order: Order): Order
  def fullInOrder(order: Order): Order
  def validateOrder(order: Order): ValidateResult
  def isOrderExist(order: Order): Boolean
  def fillMarket(order: Order)
  def fillSide(order: Order)
  def fillPrice(order: Order)
  def validateSoftCancelSign(optSign: Option[SoftCancelSign]): ValidateResult
}
