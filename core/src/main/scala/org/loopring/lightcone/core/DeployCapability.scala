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

import akka.actor._
import akka.cluster.singleton._
import akka.event.Logging
import akka.cluster._
import com.typesafe.config.Config
import scala.concurrent.duration._
import org.loopring.lightcone.core.actors._
import org.loopring.lightcone.proto.data._

trait DeployCapability {
  me: Actor with ActorLogging =>

  val config: Config
  val r: LocalRouters
  var deployed: List[String]

  implicit val cluster: Cluster
  implicit val system: ActorSystem

  def deployAllBasedOnRoles() {
    Set(
      "global_configuration_manager",
      "global_monitor",
      "cache_obsoleter",
      "blockchain_event_extractor",
      "balance_cacher",
      "balance_manager",
      "order_cacher",
      "order_read_coordinator",
      "order_update_coordinator",
      "order_updator",
      "balance_reader",
      "order_reader",
      "order_writer",
      "order_accessor",
      "order_db_accessor",
      "order_book_manager",
      "ring_finder",
      "ring_miner",
      "order_book_reader").map {
        name => ActorDeployment(name, 1, false)
      }.foreach(deploy)
  }

  def deploy(ad: ActorDeployment) {
    implicit val _ad = ad;

    ad.name match {
      case "global_configuration_manager" =>
        deploy(true, Props(new GlobalConfigurationManager(r)))

      case "global_monitor" =>
        deploy(true, Props(new GlobalMonitor(r)))

      case "cache_obsoleter" =>
        deploy(true, Props(new CacheObsoleter(r)))

      case "blockchain_event_extractor" =>
        deploy(true, Props(new BlockchainEventExtractor(r)))

      case "balance_cacher" =>
        deploy(false, Props(new BalanceCacher(r)))

      case "balance_manager" =>
        deploy(false, Props(new BalanceManager(r)))

      case "order_cacher" =>
        deploy(false, Props(new OrderCacher(r)))

      case "order_read_coordinator" =>
        deploy(false, Props(new OrderReadCoordinator(r)))

      case "order_update_coordinator" =>
        deploy(false, Props(new OrderUpdateCoordinator(r)))

      case "order_updator" =>
        deploy(false, Props(new OrderUpdater(r)))

      case "balance_reader" =>
        deploy(false, Props(new BalanceReader(r)))

      case "order_reader" =>
        deploy(false, Props(new OrderReader(r)))

      case "order_writer" =>
        deploy(false, Props(new OrderWriter(r)))

      case "order_accessor" =>
        deploy(false, Props(new OrderAccessor(r)))

      case "order_db_accessor" =>
        deploy(false, Props(new OrderDBAccessor(r)))

      case "order_book_manager" =>
        deploy(true, Props(new OrderBookManager(r)))

      case "ring_finder" =>
        deploy(true, Props(new RingFinder(r)))

      case "ring_miner" =>
        deploy(true, Props(new RingMiner(r)))

      case "order_book_reader" =>
        deploy(false, Props(new OrderBookReader(r)))

      case name =>
        log.error(s"Unknown actor $ad")
    }
  }

  private def deploy(
    isSingleton: Boolean,
    props: => Props)(implicit ad: ActorDeployment) = {

    if (ad.forceDeployment ||
      cluster.selfRoles.contains("all") ||
      cluster.selfRoles.contains(ad.name)) {

      if (isSingleton) {
        val actor = system.actorOf(
          ClusterSingletonManager.props(
            singletonProps = props,
            terminationMessage = PoisonPill,
            settings = ClusterSingletonManagerSettings(system)),
          name = ad.name)

        deployed +:= actor.path.toString
        log.info(s"deployed actor ${actor.path} as singleton")
      } else {
        (0 until ad.nrInstances) foreach { i =>
          val name = ad.name + "__" + scala.util.Random.nextInt(100000)
          val actor = system.actorOf(props, name)

          deployed +:= actor.path.toString
          log.info(s"deployed actor ${actor.path}")
        }
      }
    } else {
      log.info(s"actor deployment rejected ${ad.name}")
    }
  }
}
