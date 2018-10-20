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

import akka.actor.{ ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props }
import com.corundumstudio.socketio.Configuration
import com.google.inject.Injector

object SocketIOSystemExtension extends ExtensionId[SocketIOSystemExtensionImpl] with ExtensionIdProvider {

  override def createExtension(system: ExtendedActorSystem): SocketIOSystemExtensionImpl = {
    val router = system.actorOf(Props[SocketIOServerRouter], "socketio_router")
    new SocketIOSystemExtensionImpl(router)
  }

  override def lookup() = SocketIOSystemExtension

  override def get(system: ActorSystem): SocketIOSystemExtensionImpl = super.get(system)
}

class SocketIOSystemExtensionImpl(router: ActorRef) extends Extension {

  def init(injector: Injector): SocketIOServer = {

    val server = new com.corundumstudio.socketio.SocketIOServer(config)
    server.addConnectListener(new ConnectionListener)
    server.addDisconnectListener(new DisconnectionListener)

    new SocketIOServer(injector, server, router)
  }

  private lazy val config: Configuration = {
    val _config = new Configuration
    _config.setHostname("0.0.0.0")
    _config.setPort(9092)
    _config.setMaxFramePayloadLength(1024 * 1024)
    _config.setMaxHttpContentLength(1024 * 1024)
    _config.getSocketConfig.setReuseAddress(true)
    _config
  }

}
