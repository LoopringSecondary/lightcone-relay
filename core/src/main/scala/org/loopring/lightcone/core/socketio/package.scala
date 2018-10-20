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

package org.loopring.lightcone.core

import com.corundumstudio.socketio.SocketIOClient

package object socketio {

  //  type ProtoBuf[T] = scalapb.GeneratedMessage with scalapb.Message[T]
  //
  //  implicit def string2ProtoBuf[T <: ProtoBuf[T]](json: String)(
  //    implicit
  //    c: scalapb.GeneratedMessageCompanion[T]
  //  ): T =
  //    JsonFormat.fromJsonString[T](json)
  //
  //  implicit def protoBuf2JavaMap[T <: ProtoBuf[T]](t: T): java.util.Map[String, Any] = {
  //    val mapper = new ObjectMapper()
  //    mapper.readValue(JsonFormat.toJsonString(t), classOf[java.util.Map[String, Any]])
  //  }

  case class event(event: String, broadcast: Int, interval: Long, replyTo: String) extends scala.annotation.StaticAnnotation {

    def this(event: String) =
      this(event, broadcast = 0, interval = -1, replyTo = "")

    def this(broadcast: Int, interval: Long, replyTo: String) =
      this(event = "", broadcast = broadcast, interval = interval, replyTo = replyTo)
  }

  import scala.reflect.runtime.universe._

  // 消息class
  case class ProviderEventClass[T](instance: T, clazz: Class[_ <: T], methods: Seq[ProviderEventMethod])

  // 消息方法
  case class ProviderEventMethod(event: event, method: MethodSymbol,
    paramClazz: Option[Class[_]], futureType: Type)

  case class BroadcastMessage(server: SocketIOServer, provider: ProviderEventClass[_], method: ProviderEventMethod)

  // 客户端订阅的消息
  case class SubscriberEvent(client: SocketIOClient, event: String, json: String)

  case class StartBroadcast(server: SocketIOServer, pool: Int)

  final case class SocketIOException(
    message: String = "",
    cause: Throwable = None.orNull
  )
    extends Exception(message, cause)

}
