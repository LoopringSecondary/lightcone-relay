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

import scala.collection.mutable.ListBuffer

import org.loopring.lightcone.proto.data._
import akka.actor._
import akka.cluster.singleton._
import akka.event.Logging
import akka.cluster._
import com.typesafe.config.Config
import scala.concurrent.duration._
import org.loopring.lightcone.core.actors._

trait DeployCapability {
  me: Actor with ActorLogging =>

  val config: Config
  val r: LocalRouters
  var deployed: Set[String]

  implicit val cluster: Cluster
  implicit val system: ActorSystem

  def deploy(deployment: ActorDeployment) {
    val name = deployment.name.toLowerCase
    val size = Integer.max(1, deployment.nrInstances)

    name match {
      case "global_configuration_manager" =>
        deploySingleton(Props(new GlobalConfigurationManager(r)), name)

      case "global_monitor" =>
        deploySingleton(Props(new GlobalMonitor(r)), name)

      case "cache_obsoleter" =>
        deploySingleton(Props(new CacheObsoleter(r)), name)

      case "blockchain_event_extractor" =>
        deploySingleton(Props(new BlockchainEventExtractor(r)), name)

      case "balance_cacher" =>
        deploy(Props(new BalanceCacher(r)), name, size)

      case "balance_manager" =>
        deploy(Props(new BalanceManager(r)), name, size)

      case "order_cacher" =>
        deploy(Props(new OrderCacher(r)), name, size)

      case "order_read_coordinator" =>
        deploy(Props(new OrderReadCoordinator(r)), name, size)

      case "order_update_coordinator" =>
        deploy(Props(new OrderUpdateCoordinator(r)), name, size)

      case "order_updator" =>
        deploy(Props(new OrderUpdater(r)), name, size)

      case "balance_reader" =>
        deploy(Props(new BalanceReader(r)), name, size)

      case "order_reader" =>
        deploy(Props(new OrderReader(r)), name, size)

      case "order_writer" =>
        deploy(Props(new OrderWriter(r)), name, size)

      case "order_accessor" =>
        deploy(Props(new OrderAccessor(r)), name, size)

      case "order_db_accessor" =>
        deploy(Props(new OrderDBAccessor(r)), name, size)

      case "order_book_manager" =>
        deploySingleton(Props(new OrderBookManager(r)), name)

      case "ring_finder" =>
        deploySingleton(Props(new RingFinder(r)), name)

      case "ring_miner" =>
        deploySingleton(Props(new RingMiner(r)), name)

      case "order_book_reader" =>
        deploy(Props(new OrderBookReader(r)), name, size)

      case name =>
      //log.error(s"Unknown actor $name")
    }
  }

  private def deploySingleton(props: => Props, name: String) = {
    log.info(s"deploying $name as singleton")
    val actor = system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = props,
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system)),
      name = name)
    deployed += actor.path.toString
  }

  private def deploy(props: => Props, name: String, numGroup: Int = 1) = {
    (0 until numGroup) foreach { i =>
      val id = s"${name}_$i";
      log.info(s"deploying $id as singleton")
      val actor = system.actorOf(props, id)
      deployed += actor.path.toString
    }
  }
}
