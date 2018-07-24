package com.upblockchain.lightcone

import com.typesafe.config.ConfigFactory
import akka.actor._

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

      // Create an Akka system
      val system = ActorSystem("Lightcone", config)
      // Create an actor that handles cluster domain events
      system.actorOf(Props[ClusterListener], name = "clusterListener")
    }
  }

}
