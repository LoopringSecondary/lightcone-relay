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

import akka.actor.ActorRef
import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.listener.DataListener
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.inject.Injector

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.reflect.ClassTag

private[socketio] object SocketIOServer {

  def apply(
    injector: Injector,
    settings: SocketIOSettings,
    providers: Seq[ProviderEventClass[_]],
    server: com.corundumstudio.socketio.SocketIOServer,
    router: ActorRef
  ): SocketIOServer = {
    new SocketIOServer(injector, settings, providers, server, router)
  }

}

private[socketio] class SocketIOServer(
    injector: Injector,
    settings: SocketIOSettings,
    providers: Seq[ProviderEventClass[_]],
    server: IOServer,
    router: ActorRef
) {

  private lazy val mapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  def getProviders(fallback: Int ⇒ Boolean): Seq[ProviderEventClass[_]] = {
    providers.map { p ⇒
      val ms = p.methods.filter(e ⇒ fallback(e.event.broadcast))
      p.copy(methods = ms)
    }.filter(_.methods.nonEmpty)
  }

  def start: Unit = {

    router ! StartBroadcast(this, getProviders(_ != 0), settings.pool)

    server.addEventListener("", classOf[java.util.Map[String, Any]], new DataListener[java.util.Map[String, Any]] {
      override def onData(client: IOClient, data: java.util.Map[String, Any], ackSender: AckRequest): Unit = {
        val event = data.get("method").toString

        val json = mapper.writeValueAsString(data.get("params"))

        SocketIOClient.add(client, event, json)

        getProviders(_ == 0).foreach(replyMessage(ackSender, _, json))

      }
    })

    server.start
  }

  def replyMessage(ackSender: AckRequest, clazz: ProviderEventClass[_], json: String): Unit = {

    val methodEvent = clazz.methods.head

    val instance = clazz.instance

    val resp = invokeMethod(instance, methodEvent, Some(json))

    ackSender.sendAckData(resp)

  }

  def broadMessage(event: String, anyRef: AnyRef): Unit = {
    server.getBroadcastOperations.sendEvent(event, anyRef)
  }

  def invokeMethod[T: ClassTag](instance: T, method: ProviderEventMethod, data: Option[String]): AnyRef = {

    // 这里还有异常没处理
    val params = data.nonEmpty && method.paramClazz.nonEmpty match {
      case true ⇒
        Some(mapper.readValue(data.get, method.paramClazz.get))
      case _ ⇒ None
    }

    val mm = EventReflection.methodMirror(instance, method.method)

    // 这里获取 future
    val futureResp = params match {
      case Some(p) ⇒ mm(p)
      case _       ⇒ mm()
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

