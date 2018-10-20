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

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.reflect.runtime.universe._
import scala.util.{ Failure, Success, Try }

object EventReflection {

  lazy val currentMirror = runtimeMirror(getClass.getClassLoader)

  def annotation[T: TypeTag, A: TypeTag]: Option[Annotation] = {
    annotation[A](symbolOf[T])
  }

  def annotation[A: TypeTag](symbol: Symbol): Option[Annotation] = {
    symbol.annotations.find(_.tree.tpe =:= typeOf[A])
  }

  def methods[T: TypeTag]: Seq[Symbol] = {
    typeOf[T].decls.filter(_.isMethod).toSeq
  }

  def method[T: TypeTag](name: String): Symbol = {
    typeOf[T].decl(TermName(name))
  }

  def returnType(methodSymbol: MethodSymbol): Option[Type] = {
    if (methodSymbol.returnType.typeConstructor =:= typeOf[Future[_]].typeConstructor)
      methodSymbol.returnType.typeArgs.headOption
    else
      Some(methodSymbol.returnType)
  }

  def option2Try[T]: PartialFunction[Option[T], Try[T]] = {
    case Some(t) ⇒ Success(t)
    case _       ⇒ Failure(new Exception(""))
  }

  def parameterSingleClazz(methodSymbol: MethodSymbol): Option[Class[_]] = {
    methodSymbol.paramLists.flatMap(_.map(_.typeSignature.typeSymbol.asClass)).headOption.map {
      case classSymbol: ClassSymbol ⇒
        val fullNameSymbol = currentMirror.staticClass(classSymbol.fullName)
        currentMirror.runtimeClass(fullNameSymbol)
    }
  }

  def lookupEventMethods[T: TypeTag]: Seq[ProviderEventMethod] = {

    methods[T].map { m ⇒
      (m, annotation[event](m))
    }.filter(_._2.nonEmpty).map {
      case (m, a) ⇒
        val eventAnno = toEventAnnotation(a.get)
        val rTpe = returnType(m.asMethod)
        val pClazz = parameterSingleClazz(m.asMethod)

        eventAnno.broadcast match {
          case 0 | 1 | 2 ⇒
            val check1 = (m.asMethod.paramLists.size == 1) || (m.asMethod.paramLists.size == 0 && eventAnno.broadcast == 2)
            require(check1, s"${typeOf[T].typeSymbol.asClass.fullName}.${m.asMethod.name} parameter list not match ")
            val check2 = (eventAnno.broadcast == 2 && pClazz.nonEmpty)
            require(!check2, s"${typeOf[T].typeSymbol.asClass.fullName}.${m.asMethod.name} must have one parameter, broadcast=2 has no parameter ")
            require(rTpe.nonEmpty && !(rTpe.get =:= typeOf[Unit]), s"${typeOf[T].typeSymbol.asClass} method ${m.asMethod.name} must have no Unit type")
          case _ ⇒ throw new SocketIOException(s"${eventAnno} has wrong broadcast num, only in {0, 1, 2}")
        }
        ProviderEventMethod(event = eventAnno, method = m.asMethod, paramClazz = pClazz, futureType = rTpe.get)
    }

  }

  def getMethodMirror[T: Manifest](i: T, m: MethodSymbol): MethodMirror = {
    currentMirror.reflect(i).reflectMethod(m)
  }

  private def toEventAnnotation: PartialFunction[Annotation, event] = {
    case anno: Annotation ⇒
      // 这里是按照顺序获取的 顺序不对会报错
      anno.tree match {
        case Apply(_, Literal(Constant(name: String)) ::
          Literal(Constant(broadcast: Int)) ::
          Literal(Constant(interval: Long)) ::
          Literal(Constant(replyTo: String)) :: Nil) ⇒
          event(name, broadcast, interval, replyTo)

        case Apply(_, Literal(Constant(broadcast: Int)) ::
          Literal(Constant(interval: Long)) ::
          Literal(Constant(replyTo: String)) :: Nil) ⇒
          event("", broadcast, interval, replyTo)

        case Apply(_, Literal(Constant(name: String)) :: Nil) ⇒
          event(name, broadcast = 0, interval = -1, replyTo = "")

        case _ ⇒ throw new SocketIOException(s"${anno.tree.tpe} have wrong field order, just like event(event: String, broadcast: Int, interval: Long, replyTo: String)")

      }
  }

  private def filterAnnotation: PartialFunction[Seq[Annotation], Option[Annotation]] = {
    case annos: Seq[Annotation] ⇒ annos.find(_.tree.tpe =:= typeOf[event])
  }

  def lookupEventMethod[T](event: String): Option[ProviderEventMethod] = {
    lookupEventMethods.find(_.event.event == event)
  }

  def invokeMethod[T](instance: T, method: ProviderEventMethod, data: Option[String])(implicit m: Manifest[T]): AnyRef = {
    import org.loopring.lightcone.core.socketio.SocketIOServerLocal.mapper

    // 这里还有异常没处理
    val params = data.nonEmpty && method.paramClazz.nonEmpty match {
      case true ⇒
        Some(mapper.readValue(data.get, method.paramClazz.get))
      case _ ⇒ None
    }

    val methodMirror = getMethodMirror(instance, method.method)

    // 这里获取 future
    val futureResp = params match {
      case Some(p) ⇒ methodMirror(params)
      case _       ⇒ methodMirror()
    }

    // 这里等待返回值
    val respAny = futureResp match {
      case f: Future[_] ⇒ Await.result(f, Duration.Inf)
      case x            ⇒ x
    }

    // 这里没有考虑proto message
    respAny match {
      case r: String ⇒ r
      case r         ⇒ mapper.convertValue(r, classOf[java.util.Map[String, Any]])
    }

  }

}
