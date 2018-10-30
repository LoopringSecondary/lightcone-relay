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

import akka.actor.{ ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props }
import com.corundumstudio.socketio.Configuration
import com.google.inject.Injector
import com.typesafe.config.Config

object SocketIOSystemExtension extends ExtensionId[SocketIOSystemExtensionImpl] with ExtensionIdProvider {

  override def createExtension(system: ExtendedActorSystem): SocketIOSystemExtensionImpl = {
    val router = system.actorOf(Props[SocketIOServerRouter], "socketio_router")
    new SocketIOSystemExtensionImpl(router)
  }

  override def lookup() = SocketIOSystemExtension

  override def get(system: ActorSystem): SocketIOSystemExtensionImpl = super.get(system)
}

class SocketIOSystemExtensionImpl(router: ActorRef) extends Extension {

  import EventReflection._

  import scala.reflect.runtime.universe._

  def init(injector: Injector, settings: SocketIOSettings): SocketIOServer = {

    import net.codingwell.scalaguice.InjectorExtensions._
    val cfg = injector.instance[Config]
    val port = if (cfg.hasPath("socketio.port")) cfg.getInt("socketio.port") else settings.port
    val pool = if (cfg.hasPath("socketio.pool")) cfg.getInt("socketio.pool") else settings.pool

    val copySettings = settings.withPort(port).withPool(pool)

    val server = new IOServer(config(port))
    server.addConnectListener(new ConnectionListener)
    server.addDisconnectListener(new DisconnectionListener)

    val providers = for {
      cls ← settings.ts.foldLeft(Set.empty[Class[_]]) {
        case (set, tp) ⇒ set ++ subClass(tp)
      }.toSeq

      i = injector.getInstance(cls)

    } yield {

      val pms = membersWithNamed[event](cls).map(packingProviderEventMethod)

      ProviderEventClass(i, cls, pms)
    }

    SocketIOServer(injector, copySettings, providers, server, router)
  }

  private lazy val config = (port: Int) ⇒ {
    val _config = new Configuration
    _config.setHostname("0.0.0.0")
    _config.setPort(port)
    _config.setMaxFramePayloadLength(1024 * 1024)
    _config.setMaxHttpContentLength(1024 * 1024)
    _config.getSocketConfig.setReuseAddress(true)
    _config
  }

  private[socketio] def packingProviderEventMethod(symbol: MethodSymbol): ProviderEventMethod = {

    val e = annotation[event](symbol) {
      case Apply(_, Literal(Constant(name: String)) ::
        Literal(Constant(broadcast: Int)) ::
        Literal(Constant(interval: Long)) ::
        Literal(Constant(replyTo: String)) :: Nil) ⇒

        event(name, broadcast, interval, replyTo)

      case Apply(_, Literal(Constant(broadcast: Int)) ::
        Literal(Constant(interval: Long)) ::
        Literal(Constant(replyTo: String)) :: Nil) ⇒
        event("", broadcast, interval, replyTo)

      case Apply(_, Literal(Constant(name: String)) :: Nil) ⇒
        event(name, broadcast = 0, interval = -1, replyTo = "")

      case _ ⇒ throw new SocketIOException(s" have wrong field order, just like event(event: String, broadcast: Int, interval: Long, replyTo: String)")
    }

    val pTpe = parameterSingleClazz(symbol)

    require(
      (symbol.paramLists.size == 1) ||
        (symbol.paramLists.size == 0 && e.get.broadcast == 2),
      s"method [${symbol.fullName}] parameter list not match "
    )

    require(
      !(e.get.broadcast == 2 && pTpe.nonEmpty),
      s"method [${symbol.fullName}]  must have one parameter, broadcast=2 has no parameter "
    )

    val rTpe = returnType(symbol)
    require(
      rTpe.nonEmpty && !(rTpe.get =:= typeOf[Unit]),
      s" method [${symbol.fullName}] must have no Unit type"
    )

    ProviderEventMethod(e.get, symbol, pTpe, rTpe.get)
  }
}
