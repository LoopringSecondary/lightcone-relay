package org.loopring.lightcone.core.socketio

import scala.reflect.runtime.universe._

object SocketIOSettings {

  def apply(): SocketIOSettings = new SocketIOSettings(9077, 10, "org.loopring.lightcone")

  def apply(port: Int, pool: Int): SocketIOSettings = new SocketIOSettings(port, pool, "org.loopring.lightcone")

}

class SocketIOSettings(val port: Int, val pool: Int, val projectPackage: String, val ts: TypeTag[_]*) {

  def register[T: TypeTag]: SocketIOSettings = copy(port = port, pool = pool, projectPackage = this.projectPackage, ts = typeTag[T])

  def withPort(port: Int): SocketIOSettings = copy(port = port, pool = this.pool, projectPackage = this.projectPackage)

  def withPool(pool: Int): SocketIOSettings = copy(port = this.port, pool = pool, projectPackage = this.projectPackage)

  def withProjectPackage(projectPackage: String): SocketIOSettings =
    copy(port = this.port, pool = this.pool, projectPackage = projectPackage)

  private def copy(port: Int, pool: Int, projectPackage: String, ts: TypeTag[_]*): SocketIOSettings = {
    new SocketIOSettings(port, pool, projectPackage, (this.ts ++ ts): _*)
  }

}
