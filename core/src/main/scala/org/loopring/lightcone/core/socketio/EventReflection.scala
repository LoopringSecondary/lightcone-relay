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

package org.loopring.lightcone.core.socketio

import org.reflections.Reflections

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object EventReflection {

  private lazy val defPackage = "org.loopring.lightcone"

  private lazy val m = runtimeMirror(getClass.getClassLoader)

  def tpe2Symbol: PartialFunction[TypeTag[_], Symbol] = {
    case t: TypeTag[_] ⇒ t.tpe.typeSymbol
  }

  def symbol2ClassSymbol: PartialFunction[Symbol, ClassSymbol] = {
    case s: Symbol ⇒ s.asClass
  }

  def symbol2MethodSymbol: PartialFunction[Symbol, MethodSymbol] = {
    case s: Symbol ⇒ s.asMethod
  }

  def symbol2Class: PartialFunction[ClassSymbol, Class[_]] = {
    case cs: ClassSymbol ⇒ m.runtimeClass(cs)
  }

  def class2Symbol: PartialFunction[Class[_], ClassSymbol] = {
    case cls: Class[_] ⇒ m.classSymbol(cls)
  }

  def tpe2Class: PartialFunction[TypeTag[_], Class[_]] =
    tpe2Symbol andThen symbol2ClassSymbol andThen symbol2Class

  def symbol2Tpe: PartialFunction[Symbol, Type] = {
    case s: Symbol ⇒ s.typeSignature
  }

  def subClassSymbol(t: TypeTag[_], projectPackage: String = defPackage): Set[ClassSymbol] = {
    subClass(t, projectPackage).map(class2Symbol)
  }

  def subClass(t: TypeTag[_], projectPackage: String = defPackage): Set[Class[_]] = {
    val r = new Reflections(projectPackage)
    import scala.collection.JavaConverters._
    r.getSubTypesOf(tpe2Class(t)).asScala.toSet
  }

  def members(clazz: Class[_]): Seq[MethodSymbol] = members(class2Symbol(clazz))

  def members(symbol: ClassSymbol): Seq[MethodSymbol] =
    symbol2Tpe(symbol).decls.map(_.asMethod).toSeq

  def membersWithNamed[A: TypeTag](cls: Class[_]): Seq[MethodSymbol] = {
    println(members(cls))
    members(cls).map {
      case m: MethodSymbol ⇒
        println("====>>>" + memberNamed[A](m))
        (m, memberNamed[A](m))
    }.filter(_._2.nonEmpty).map(_._1)
  }

  def memberNamed[A: TypeTag](symbol: Symbol): Option[Annotation] = {
    symbol.annotations.find(_.tree.tpe =:= typeOf[A])
  }

  def annotation[A: TypeTag](symbol: MethodSymbol)(fallback: Tree ⇒ A): Option[A] = {
    memberNamed(symbol).map(a ⇒ fallback(a.tree))
  }

  def returnType(symbol: MethodSymbol): Option[Type] = {
    if (symbol.returnType.typeConstructor =:= typeOf[Future[_]].typeConstructor)
      symbol.returnType.typeArgs.headOption
    else
      Some(symbol.returnType)
  }

  def parameterSingleClazz(symbol: MethodSymbol): Option[Class[_]] = {
    symbol.paramLists.flatMap(_.map(_.typeSignature.typeSymbol.asClass)).headOption.map(symbol2Class)
  }

  def methodMirror[T: ClassTag](i: T, methodSymbol: MethodSymbol): MethodMirror = {
    m.reflect(i).reflectMethod(methodSymbol)
  }

}
