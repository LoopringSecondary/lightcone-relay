package org.loopring.lightcone.core.socketio

import akka.actor.{ ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }

object SocketIOSystemExtension extends ExtensionId[SocketIOSystemExtensionImpl] with ExtensionIdProvider {


  override def createExtension(system: ExtendedActorSystem): SocketIOSystemExtensionImpl = {
    new SocketIOSystemExtensionImpl
  }

  override def lookup() = SocketIOSystemExtension

  override def get(system: ActorSystem): SocketIOSystemExtensionImpl = super.get(system)
}

class SocketIOSystemExtensionImpl(port: Int = 9092) extends Extension {

  

}
