/*
 * Copyright 2018 lightcore-relay
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

import org.loopring.lightcone.proto.ring._
import scala.concurrent.Future
import org.loopring.lightcone.lib.math.Rational
import org.loopring.lightcone.proto.order._

case class OrderFill(
    rawOrder: RawOrder,
    sPrice: Rational,
    rateAmountS: Rational,
    fillAmountS: Rational,
    fillAmountB: Rational,
    reduceRate: Rational,
    receivedFiat: Rational = Rational(0),
    feeSelection: Byte = 0.toByte
)

case class RingCandidate(
    rawRing: Ring,
    receivedFiat: Rational = Rational(0),
    gasPrice: BigInt = BigInt(0),
    gasLimit: BigInt = BigInt(0),
    orderFills: Map[String, OrderFill] = Map()
)

trait RingEvaluator {
  def getRingCadidatesToSettle(candidates: RingCandidates): Future[Seq[RingCandidate]]
}
