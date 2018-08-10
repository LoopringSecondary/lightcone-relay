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

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import akka.actor._
import akka.cluster._
import org.loopring.lightcone.core._

object Main {

  case class CmdOptions(
    port: Int = 0,
    seeds: Seq[String] = Seq.empty[String],
    roles: Seq[String] = Seq.empty[String],
    configFile: String = "")

  def main(args: Array[String]): Unit = {

    new scopt.OptionParser[CmdOptions]("lightcone") {
      head("Lightcone", "0.1")

      opt[Int]('p', "port")
        .action { (v, options) =>
          options.copy(port = v)
        }
        .text("port of this acter system")

      arg[String]("<seeds>...").unbounded().optional()
        .action { (v, options) =>
          options.copy(seeds = options.seeds :+ v.trim)
        }
        .text("cluster seed nodes")

      arg[String]("<roles>...").unbounded().optional()
        .action { (v, options) =>
          options.copy(roles = options.roles :+ v.trim)
        }
        .validate { role =>
          if (role.isEmpty) failure("arg <roles> must be provided")
          else success
        }
        .text("node roles")

      opt[String]('c', "config")
        .action { (v, options) =>
          options.copy(configFile = v.trim)
        }
        .text("path to configuration file")

    }.parse(args, CmdOptions()) match {
      case None =>

      case Some(options) =>
        val seedNodes = options.seeds
          .filter(_.nonEmpty)
          .map(s => s""""akka.tcp://Lightcone@$s"""")
          .mkString("[", ",", "]")

        val roles =
          if (options.roles.isEmpty) "[all]"
          else options.roles
            .filter(_.nonEmpty)
            .mkString("[", ",", "]")

        val hostname = InetAddress.getLocalHost.getHostAddress

        val fallback = if (options.configFile.trim.nonEmpty) {
          ConfigFactory.load(options.configFile.trim)
        } else {
          ConfigFactory.load()
        }

        val config = ConfigFactory
          .parseString(
            s"""
            akka.remote.netty.tcp.port=${options.port}
            akka.remote.netty.tcp.hostname=$hostname
            akka.cluster.roles=$roles
            akka.cluster.seed-nodes=$seedNodes
            """)
          .withFallback(fallback)

        implicit val system = ActorSystem("Lightcone", config)
        implicit val cluster = Cluster(system)

        system.actorOf(Props(new NodeManager(config)))

        println(
          """
___                __      __
/\_ \    __        /\ \    /\ \__
\//\ \  /\_\     __\ \ \___\ \ ,_\   ___    ___     ___      __
  \ \ \ \/\ \  /'_ `\ \  _ `\ \ \/  /'___\ / __`\ /' _ `\  /'__`\
   \_\ \_\ \ \/\ \L\ \ \ \ \ \ \ \_/\ \__//\ \L\ \/\ \/\ \/\  __/
   /\____\\ \_\ \____ \ \_\ \_\ \__\ \____\ \____/\ \_\ \_\ \____\
   \/____/ \/_/\/___L\ \/_/\/_/\/__/\/____/\/___/  \/_/\/_/\/____/
                 /\____/
                 \_/__/
""")
    }
  }
}
