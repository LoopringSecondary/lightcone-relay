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

import akka.actor._
import org.loopring.lightcone.proto.common.StartNewRound
import scala.concurrent.duration._
import scala.concurrent.Future

trait RepeatedJobActor extends Actor {
  var cancelOpt: Option[Cancellable] = None
  var scheduleDelay: Long = 0l
  var inited = false
  import context.dispatcher

  def initAndStartNextRound(scheduleDelay: Long): Unit = {
    this.scheduleDelay = scheduleDelay
    this.inited = true
    nextRound(0l)
  }

  def nextRound(lastRoundTime: Long): Unit = {
    if (!inited) return
    cancelOpt.foreach(_.cancel())
    val delay = scheduleDelay - (System.currentTimeMillis - lastRoundTime)
    if (delay > 0)
      cancelOpt = Some(
        context.system.scheduler.scheduleOnce(
          scheduleDelay millis,
          self,
          StartNewRound()
        )
      )
    else {
      cancelOpt = None
      self ! StartNewRound()
    }
  }

  def handleRepeatedJob(): Future[Unit]

  def receive: Receive = {
    case StartNewRound ⇒ for {
      lastTime ← Future.successful(System.currentTimeMillis)
      _ ← handleRepeatedJob()
    } yield {
      nextRound(lastTime)
    }
  }
}
