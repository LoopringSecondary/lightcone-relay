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

package org.loopring.lightcone.core.managing

import akka.actor._
import scala.concurrent.duration._
import org.loopring.lightcone.data.deployment._

class LocalActorsDetector(pattern: String) extends Actor {

  val deadline = Deadline.now + 1.second
  val max = 1000
  val selection = context.actorSelection(pattern)
  var count = 0
  var result = Set.empty[String]
  var sendResultTo: ActorRef = null

  def receive = {
    case "detect" =>
      sendResultTo = sender
      context.setReceiveTimeout(deadline.timeLeft)
      selection ! Identify(None)

    case ActorIdentity(_, refOption) =>
      count += 1
      refOption foreach { result += _.toString }
      context.setReceiveTimeout(deadline.timeLeft)
      if (count == max || deadline.isOverdue)
        completeResult()

    case ReceiveTimeout =>
      completeResult()
  }

  def completeResult(): Unit = {
    sendResultTo ! LocalActors(result.toSeq)
    context.stop(self)
  }
}