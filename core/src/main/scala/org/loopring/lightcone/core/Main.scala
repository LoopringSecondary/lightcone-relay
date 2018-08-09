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

package org.loopring.lightcone.core

import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.cluster._
import org.loopring.lightcone.core.utils._

object Main {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq("29090", "29091", "0"))
    else
      startup(args)

  }

  def startup(ports: Seq[String]): Unit = {
    ports foreach { port =>
      // Override the configuration of the port
      val config = ConfigFactory
        .parseString(s"""
        akka.remote.netty.tcp.port=$port
        """)
        // .withFallback(ConfigFactory.parseString("akka.cluster.roles = [compute]"))
        .withFallback(ConfigFactory.load())

      // Create an Akka Cluster
      implicit val system = ActorSystem("Lightcone", config)
      implicit val cluster = Cluster(system)

      system.actorOf(Props(new ActorDeployer(config, "")), name = "deployer")

      new http.InternalWebServer().start()
    }
  }

}
