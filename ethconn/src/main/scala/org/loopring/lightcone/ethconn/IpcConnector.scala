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

package org.loopring.ethcube

import akka.actor._
import akka.routing._
import scalapb.json4s.JsonFormat
import jnr.unixsocket._
import java.io._
import java.nio.channels.Channels
import java.nio.CharBuffer
import org.loopring.ethcube.proto.data._

private class IpcConnector(node: EthereumProxySettings.Node)
  extends Actor
  with ActorLogging {

  import context.dispatcher

  val address = new UnixSocketAddress(new File(node.ipcPath))
  val channel = UnixSocketChannel.open(address)

  val writer = new PrintWriter(Channels.newOutputStream(channel))
  val reader = new InputStreamReader(Channels.newInputStream(channel))

  def receive: Receive = {
    case req: JsonRpcReq ⇒

      try {
        writer.print(JsonFormat.toJsonString(req))
        writer.flush()

        val result = CharBuffer.allocate(1024)
        reader.read(result)
        result.flip()
        log.debug(s"ipc response raw: ${result}")

        // val response = JsonFormat.fromJsonString[JsonRpcRes](result.toString)
        sender ! JsonRpcRes(result.toString())
      } catch {
        case e: Throwable ⇒ log.error(e.getMessage)
      }

  }
}
