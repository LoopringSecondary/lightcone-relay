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

package org.loopring.lightcone.gateway

import scala.reflect.runtime.universe._

package object socketio {

  type IOServer = com.corundumstudio.socketio.SocketIOServer

  type IOClient = com.corundumstudio.socketio.SocketIOClient

  // broadcat => 0: 不广播直接回复请求者, 1: 广播订阅者, 2: 主动给广播所有(这种情况event无效)
  case class event(event: String, broadcast: Int, interval: Long, replyTo: String) extends scala.annotation.StaticAnnotation {

    def this(event: String) =
      this(event, broadcast = 0, interval = -1, replyTo = "")

    def this(broadcast: Int, interval: Long, replyTo: String) =
      this(event = "", broadcast = broadcast, interval = interval, replyTo = replyTo)
  }

  case class ProviderEventClass[T](instance: T, clazz: Class[_ <: T], methods: Seq[ProviderEventMethod])

  case class ProviderEventMethod(event: event, method: MethodSymbol,
      paramClazz: Option[Class[_]], futureType: Type)

  // 客户端订阅的消息
  case class SubscriberEvent(client: IOClient, event: String, json: String)

  case class BroadcastMessage(server: SocketIOServer, provider: ProviderEventClass[_], method: ProviderEventMethod)

  case class StartBroadcast(server: SocketIOServer, providers: Seq[ProviderEventClass[_]], pool: Int)

  final case class SocketIOException(
      message: String = "",
      cause: Throwable = None.orNull
  )
    extends Exception(message, cause)

}
