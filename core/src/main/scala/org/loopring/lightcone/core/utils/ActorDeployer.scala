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

package org.loopring.lightcone.core.utils

import scala.collection.mutable.ListBuffer

import akka.actor._
import akka.cluster.singleton._
import akka.event.Logging
import akka.cluster._
import com.typesafe.config.Config
import scala.concurrent.duration._
import org.loopring.lightcone.core.actors._

class ActorDeployer(
  config: Config,
  hostname: String)(implicit cluster: Cluster)
  extends Actor with ActorLogging {

  implicit val system = cluster.system
  val paths = new ListBuffer[String]

  deploy()

  def receive: Receive = {
    case _ =>
  }

  def deploy(): LocalRouters = {
    val r = new LocalRouters()

    deploySingleton(
      Props(new GlobalConfigurationManager(r)),
      "global_configuration_manager")

    deploySingleton(
      Props(new GlobalMonitor(r)),
      "global_monitor")

    deploySingleton(
      Props(new CacheObsoleter(r)),
      "cache_obsoleter")

    deploySingleton(
      Props(new BlockchainEventExtractor(r)),
      "blockchain_event_extractor")

    deploy(
      Props(new BalanceCacher(r)),
      "balance_cacher", 5)

    deploy(
      Props(new BalanceManager(r)),
      "balance_manager", 5)

    deploy(
      Props(new OrderCacher(r)),
      "order_cacher", 5)

    deploy(
      Props(new OrderReadCoordinator(r)),
      "order_read_coordinator", 5)

    deploy(
      Props(new OrderUpdateCoordinator(r)),
      "order_update_coordinator", 5)

    deploy(
      Props(new OrderUpdater(r)),
      "order_updator", 5)

    deploy(
      Props(new BalanceReader(r)),
      "balance_reader", 5)

    deploy(
      Props(new OrderReader(r)),
      "order_reader", 5)

    deploy(
      Props(new OrderWriter(r)),
      "order_writer", 5)

    deploy(
      Props(new OrderAccessor(r)),
      "order_accessor", 5)

    deploy(
      Props(new OrderDBAccessor(r)),
      "order_db_accessor", 5)

    deploySingleton(
      Props(new OrderBookManager(r)),
      "order_book_manager")

    deploySingleton(
      Props(new RingFinder(r)),
      "ring_finder")

    deploySingleton(
      Props(new RingMiner(r)),
      "ring_miner")

    deploy(
      Props(new OrderBookReader(r)),
      "order_book_manager", 1)

    r
  }

  private def deploySingleton(props: => Props, name: String) = {
    if (cluster.selfRoles.contains("*") ||
      cluster.selfRoles.contains(name)) {
      val actor = system.actorOf(
        ClusterSingletonManager.props(
          singletonProps = props,
          terminationMessage = PoisonPill,
          settings = ClusterSingletonManagerSettings(system)),
        name = name)
      paths += actor.path.toStringWithoutAddress
    }
  }

  private def deploy(props: => Props, name: String, numGroup: Int = 1) = {
    if (cluster.selfRoles.contains("*") ||
      cluster.selfRoles.contains(name)) {
      (0 until numGroup) foreach { i =>
        val actor = system.actorOf(props, s"${name}_$i")
        paths += actor.path.toStringWithoutAddress
      }
    }
  }
}
