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

import akka.actor.{ Actor, ActorLogging }

class SocketIOServerActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case BroadcastMessage(server, provider, method) ⇒

      log.info(s"${context.self.path} broadcast message")

      val event = method.event

      event.broadcast match {
        case 1 ⇒

          SocketIOClient.getClients(_ == event.event).foreach { c ⇒
            val resp = server.invokeMethod(provider.instance, method, Some(c.json))
            c.client.sendEvent(event.replyTo, resp)
          }

        case 2 ⇒

          // 主动广播给所有的client(这里不能有请求值)
          val resp = server.invokeMethod(provider.instance, method, None)
          server.broadMessage(event.replyTo, resp)
        case _ ⇒ // ignore throw new SocketIOException("broadcast not supported")
      }
  }
}
