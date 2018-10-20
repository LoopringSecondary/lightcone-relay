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

import akka.actor.{ Actor, ActorLogging, Props, Timers }
import akka.routing.RoundRobinPool

import scala.concurrent.duration._

class SocketIOServerRouter extends Actor with Timers with ActorLogging {

  implicit val ex = context.system.dispatcher

  override def receive: Receive = {
    case StartBroadcast(server, pool) ⇒

      log.info("start check broadcast message")

      import SocketIOServerLocal._

      val router = context.actorOf(
        RoundRobinPool(pool).props(Props[SocketIOServerActor]),
        "socketio_actor"
      )

      for {

        provider ← getProviders.map { p ⇒
          // 去掉不需要广播的消息
          val ms = p.methods.filterNot(_.event.broadcast == 0)
          p.copy(methods = ms)
        }

        m ← provider.methods

      } yield {
        val broadcast = BroadcastMessage(server, provider, m)
        context.system.scheduler.schedule(2 seconds, m.event.interval seconds, router, broadcast)
      }
  }

}
