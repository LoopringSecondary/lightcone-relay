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
    case b: BroadcastMessage ⇒

      log.info(s"${context.self.path} broadcast message")

      import SocketIOServerLocal._

      val event = b.method.event

      val provider = b.provider

      event.broadcast match {
        case 1 ⇒ // getSubscriber(m.event.event) // 发送给订阅者
          getSubscriber(event.event).foreach { c ⇒
            val resp = EventReflection.invokeMethod(provider.instance, b.method, Some(c.json))
            c.client.sendEvent(event.replyTo, resp)
          }
        case 2 ⇒
          // 主动广播给所有的client(这里不能有请求值)
          val resp = EventReflection.invokeMethod(provider.instance, b.method, None)
          b.server.broadMessage(event.replyTo, resp)
        case _ ⇒ // throw new Exception("broadcast not supported")
      }
  }

}
