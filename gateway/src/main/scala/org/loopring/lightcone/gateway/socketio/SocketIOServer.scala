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

package org.loopring.lightcone.gateway.socketio

import akka.actor.{ ActorSystem, Props }
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.{ AckRequest, Configuration }
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.Config
import org.loopring.lightcone.gateway.jsonrpc.JsonRpcService
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SocketIOServer(
    jsonRpcService: JsonRpcService,
    config: Config
)(
    implicit
    system: ActorSystem
) {

  lazy val logger = LoggerFactory.getLogger(getClass)

  private lazy val port = config.getInt("jsonrpc.socketio.port")

  private lazy val mapper = {
    val _mapper = new ObjectMapper()
    _mapper.registerModule(DefaultScalaModule)
    _mapper
  }

  private lazy val ioConfig = {
    val _config = new Configuration
    _config.setHostname("0.0.0.0")
    _config.setPort(port)
    _config.setMaxFramePayloadLength(1024 * 1024)
    _config.setMaxHttpContentLength(1024 * 1024)
    _config.getSocketConfig.setReuseAddress(true)
    _config
  }

  private lazy val router = system.actorOf(Props[SocketIOServerRouter], "socketio_router")

  def start: Unit = {

    val server = new IOServer(ioConfig)
    server.addConnectListener(new ConnectionListener)
    server.addDisconnectListener(new DisconnectionListener)

    server.addEventListener("", classOf[java.util.Map[String, Any]], new DataListener[java.util.Map[String, Any]] {
      override def onData(client: IOClient, data: java.util.Map[String, Any], ackSender: AckRequest): Unit = {

        val event = data.get("method").toString

        val json = mapper.writeValueAsString(data)

        logger.info(s"${client.getRemoteAddress} request: ${data}")

        SocketIOClient.add(client, event, json)

        invoke(json).foreach(ackSender.sendAckData(_))

      }
    })

    router ! StartBroadcast(
      this,
      jsonRpcService.registering,
      config.getInt("jsonrpc.socketio.pool")
    )

    server.start

    logger.info(s"socketio server started @ ${port}")
  }

  def invoke(json: String) = {

    Await.result(jsonRpcService.getAPIResult(json), Duration.Inf).map {
      json ⇒

        val resp = mapper.readValue(json, classOf[java.util.Map[String, Any]])

        logger.info(s"socketio rpc response: ${resp}")

        resp
    }
  }

}

