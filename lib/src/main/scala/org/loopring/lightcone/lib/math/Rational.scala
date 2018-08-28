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

package org.loopring.lightcone.lib.math

import java.math.{MathContext, RoundingMode}

import scala.math.{Ordered, ScalaNumber, ScalaNumericConversions}

final class Rational(num:BigInt, denom:BigInt)
  extends ScalaNumber with ScalaNumericConversions with Serializable with Ordered[Rational] {
  require( denom.signum != 0)

  val defaultMathContext = MathContext.DECIMAL128

  def +(that:Rational) = {
    new Rational(num = this.num+that.num, denom=this.denom+that.denom)
  }

  def -(that:Rational) = {
    new Rational(num = (this.num*that.denom)-(this.denom*that.num), denom=this.denom*that.denom)
  }

  def /(that:Rational) = {
    new Rational(num = this.num*that.denom, denom=this.denom*that.num)
  }

  def *(that:Rational) = {
    new Rational(num = this.num*that.num, denom=this.denom*that.denom)
  }

  override def underlying(): AnyRef = this

  override def compare(that: Rational): Int = this.num*that.denom compareTo this.denom*that.num

  override def isWhole(): Boolean = true

  override def intValue(): Int = {
    (this.num/this.denom).intValue()
  }

  override def longValue(): Long = {
    (BigDecimal(this.num)/BigDecimal(this.denom)).longValue()
  }

  override def floatValue(): Float = {
    (BigDecimal(this.num)/BigDecimal(this.denom)).floatValue()
  }

  override def doubleValue(): Double = {
    (BigDecimal(this.num)/BigDecimal(this.denom)).doubleValue()
  }

  override def toString: String = s"numerator:${this.num.toString()}, denominator:${this.denom.toString()}"

  def floatString(precision:Int): String = {
    val mc = new MathContext(precision, RoundingMode.HALF_EVEN)
    (BigDecimal(this.num, mc)/BigDecimal(this.denom, mc)).toString()
  }
}

