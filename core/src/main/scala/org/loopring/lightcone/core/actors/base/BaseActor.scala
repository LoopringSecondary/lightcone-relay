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

package org.loopring.lightcone.core.actors.base

import akka.actor.Actor

import scala.concurrent.Future
import scala.util.{ Failure, Success }

trait BaseActor extends Actor {
  import context.dispatcher

  def baseReceiver: PartialFunction[Any, Any]

  final override def receive: Receive = {
    case a: Any ⇒
      try {
        val f = baseReceiver(a)
        f match {
          case f1: Future[Any] ⇒
            f1.onComplete({
              case Failure(exception) ⇒
              case Success(value)     ⇒
            })
          case _ ⇒
        }
      } catch {
        case e: MatchError    ⇒
        case e: InternalError ⇒
      }
  }
}
