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

import java.math.{ MathContext, RoundingMode }

import scala.math.{ BigInt, Ordered, ScalaNumber, ScalaNumericConversions }

final class Rational(numerator: BigInt, denominator: BigInt)
  extends ScalaNumber with ScalaNumericConversions with Serializable with Ordered[Rational] {
  require(denominator.signum != 0)
  private val gcd = if (numerator.signum == 0) BigInt(1) else numerator gcd denominator
  val num: BigInt = numerator / gcd
  val denom: BigInt = denominator / gcd

  val defaultMathContext = MathContext.DECIMAL128

  def +(that: Rational) = {
    new Rational(
      numerator = this.num * that.denom + (this.denom * that.num),
      denominator = this.denom * that.denom)
  }

  def -(that: Rational) = {
    new Rational(
      numerator = (this.num * that.denom) - (this.denom * that.num),
      denominator = this.denom * that.denom)
  }

  def /(that: Rational) = {
    new Rational(
      numerator = this.num * that.denom,
      denominator = this.denom * that.num)
  }

  def *(that: Rational) = {
    new Rational(
      numerator = this.num * that.num,
      denominator = this.denom * that.denom)
  }

  def min(that: Rational): Rational = if (this.num * that.denom > this.denom * that.num) that else this

  def max(that: Rational): Rational = if (this.num * that.denom > this.denom * that.num) this else that

  def pow(exp: Rational) = {
    math.pow(this.doubleValue(), exp.doubleValue())
  }

  def signum: Int = this.num.signum * this.denom.signum

  override def underlying(): AnyRef = this

  override def compare(that: Rational): Int = this.num * that.denom compareTo this.denom * that.num

  override def isWhole(): Boolean = true

  override def intValue(): Int = {
    (this.num / this.denom).intValue()
  }

  override def longValue(): Long = {
    (BigDecimal(this.num) / BigDecimal(this.denom)).longValue()
  }

  override def floatValue(): Float = {
    (BigDecimal(this.num) / BigDecimal(this.denom)).floatValue()
  }

  override def doubleValue(): Double = {
    (BigDecimal(this.num) / BigDecimal(this.denom)).doubleValue()
  }

  override def toString: String = s"${this.num.toString()}/${this.denom.toString()}"

  def floatString(precisionOpt: Option[Int] = None): String = {
    val mc = precisionOpt match {
      case None => defaultMathContext
      case Some(precision) => new MathContext(precision, RoundingMode.HALF_EVEN)
    }
    (BigDecimal(this.num, mc) / BigDecimal(this.denom, mc)).toString()
  }

}

object Rational {

  val MaxDoubleValue = BigDecimal(Double.MaxValue)

  def apply(numerator: BigInt, denominator: BigInt) = new Rational(numerator, denominator)

  def apply(value: Double) = new Rational(
    (MaxDoubleValue * BigDecimal(value)).toBigInt(),
    MaxDoubleValue.toBigInt())

  def apply(numerator: BigInt) = new Rational(numerator, BigInt(1))

  def apply(numerator: Int, denominator: Int) = new Rational(BigInt(numerator), BigInt(denominator))

  def apply(numerator: Int) = new Rational(BigInt(numerator), BigInt(1))

}
