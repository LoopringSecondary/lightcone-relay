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

package org.loopring.lightcone.ethconn

import akka.actor._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config._
import org.loopring.lightcone.ethconn.proto.data.EthereumProxySettings
import org.slf4j.LoggerFactory

object Main extends App {

  import collection.JavaConverters._
  lazy val log = LoggerFactory.getLogger(getClass)

  val config = ConfigFactory.load()

  implicit val system = ActorSystem("ethcube", config)
  implicit val materializer = ActorMaterializer()

  val settings: EthereumProxySettings = {
    val sub = config.getConfig("ethereum-proxy")

    EthereumProxySettings(
      sub.getInt("pool-size"),
      sub.getInt("check-interval-seconds"),
      sub.getDouble("healthy-threshold").toFloat,
      sub.getConfigList("nodes").asScala map {
        c ⇒
          EthereumProxySettings.Node(
            c.getString("host"),
            c.getInt("port"),
            c.getString("ipcpath")
          )
      }
    )
  }

  val ethreumProxy = system.actorOf(
    Props(new EthereumProxy(settings)),
    "ethereum_proxy"
  )

  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  val endpoints = new EthereumProxyEndpoints(ethreumProxy)

  Http().bindAndHandle(endpoints.getRoutes, host, port)

  log.info(s"ethcube started with http service at ${host}:${port}")
}
