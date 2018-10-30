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
