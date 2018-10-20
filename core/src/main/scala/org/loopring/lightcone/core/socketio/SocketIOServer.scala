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

import java.util

import akka.actor.ActorRef
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.{ AckRequest, SocketIOClient }
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.inject.Injector
import org.slf4j.LoggerFactory

import scala.reflect.runtime.universe._

private[socketio] object SocketIOServerLocal {

  private var eventProvider: Seq[ProviderEventClass[_]] = Seq.empty

  private var eventSubscriber: Set[SubscriberEvent] = Set.empty

  lazy val mapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  def addSubscriber(s: SubscriberEvent): Unit =
    eventSubscriber = eventSubscriber + s

  def getSubscriber(event: String): Seq[SubscriberEvent] =
    eventSubscriber.filter(_.event == event).toSeq

  def getSubscribers: Seq[SubscriberEvent] = eventSubscriber.toSeq

  def addProvider(t: ProviderEventClass[_]): Unit = {
    eventProvider = eventProvider :+ t
  }

  def getProviders: Seq[ProviderEventClass[_]] = eventProvider

  def getProvider(name: String, broadcast: Int = 0): Seq[ProviderEventClass[_]] = {

    eventProvider.map { e ⇒
      // TODO(Toan) 这里会不会有同一个类多个方法注册了相同的事件
      // 相同事件 应该只有一个
      val ms = e.methods.find { e ⇒
        e.event.event == name && e.event.broadcast == broadcast
      } match {
        case Some(m) ⇒ Seq(m)
        case _       ⇒ Seq.empty
      }
      e.copy(methods = ms)
    }.filter(_.methods.nonEmpty) // .headOption

  }
}

class SocketIOServer(
    injector: Injector,
    server: com.corundumstudio.socketio.SocketIOServer,
    router: ActorRef,
    pool: Int
) {

  lazy val logger = LoggerFactory.getLogger(getClass)

  import SocketIOServerLocal._

  // 这里分两种情况 1. 直接注册listener, 2. 需要注入到actor
  def register[T: TypeTag]: Unit = {

    val tag = typeTag[T]
    val runtimeClass = typeTag[T].mirror.runtimeClass(tag.tpe)

    val instance = injector.getInstance(runtimeClass)
    val methods = EventReflection.lookupEventMethods
    logger.info(s"SocketIO: ${runtimeClass} has bean registered")

    val t = ProviderEventClass(instance = instance, clazz = runtimeClass, methods = methods)

    addProvider(t)
  }

  /** 1 客户端直接请求数据
   *  2 客户端请求广播数据(这种情况先不考虑)
   *  3 客户端订阅广播数据
   *  4 主动广播消息到客户端(暂时不支持这种情况)
   */
  def start: Unit = {

    router ! StartBroadcast(this, pool)

    // 注册请求事件
    server.addEventListener("", classOf[java.util.Map[String, Any]], new DataListener[java.util.Map[String, Any]] {
      override def onData(client: SocketIOClient, data: util.Map[String, Any], ackSender: AckRequest): Unit = {
        val event = data.get("method").toString

        val json = mapper.writeValueAsString(data.get("params"))

        addSubscriber(SubscriberEvent(client, event, json))

        // 注册直接回复的消息
        getProvider(event).foreach { clazz ⇒
          replyMessage(ackSender, clazz, json)
        }
      }
    })

    server.start()
  }

  def replyMessage(ackSender: AckRequest, clazz: ProviderEventClass[_], json: String): Unit = {

    val methodEvent = clazz.methods.head

    val instance = clazz.instance

    val resp = EventReflection.invokeMethod(instance, methodEvent, Some(json))

    ackSender.sendAckData(resp)

  }

  def getAllClients: Seq[SocketIOClient] = {
    import scala.collection.JavaConverters._
    server.getAllClients.asScala.toSeq
  }

  def broadMessage(event: String, anyRef: AnyRef): Unit = {
    server.getBroadcastOperations.sendEvent(event, anyRef)
  }

  def stop: Unit = server.stop()

  def sendErrorMessage(ackSender: AckRequest): Unit = {
    ackSender.sendAckData("error")
  }

}
