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

package org.loopring.lightcone.core

import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.order.Order
import org.loopring.lightcone.lib.etypes._

package object richproto {
  implicit class RichOrder(order: Order) {
    def getRemained(): (Rational, Rational) = {
      if (order.rawOrder.get.buyNoMoreThanAmountB) {
        val sellPrice = order.sellPrice()
        val remainedAmountB = Rational(
          order.rawOrder.get.amountB.asBigInt - (
            order.dealtAmountS.asBigInt +
            order.cancelledAmountB.asBigInt +
            order.splitAmountB.asBigInt
          )
        )
        (remainedAmountB * sellPrice, remainedAmountB)
      } else {
        val remainedAmountS = Rational(
          order.rawOrder.get.amountS.asBigInt - (
            order.dealtAmountS.asBigInt +
            order.cancelledAmountS.asBigInt +
            order.splitAmountS.asBigInt
          )
        )
        val buyPrice = order.buyPrice()
        (remainedAmountS, remainedAmountS * buyPrice)
      }
    }

    def getSignerAddr(): String = ???

    def sellPrice(): Rational = {
      Rational(order.rawOrder.get.amountS.asBigInt, order.rawOrder.get.amountB.asBigInt)
    }
    def buyPrice(): Rational = {
      Rational(order.rawOrder.get.amountB.asBigInt, order.rawOrder.get.amountS.asBigInt)
    }
  }

}
