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

package org.loopring.lightcone.core.routing

import akka.actor._
import akka.cluster._
import akka.cluster.routing._
import akka.routing._
import akka.cluster.singleton._
import com.typesafe.config.Config
import org.loopring.lightcone.data.deployment._

class Routers(config: Config)(implicit cluster: Cluster) {
  implicit val system = cluster.system

  lazy val clusterManager = routerForSingleton("cluster_manager")
  lazy val cacheObsoleter = routerForSingleton("cache_obsoleter")
  lazy val blockchainEventExtractor = routerForSingleton("blockchain_event_extractor")

  lazy val balanceCacher = routerFor("balance_cacher")
  lazy val balanceManager = routerFor("balance_manager")
  lazy val orderCacher = routerFor("order_cacher")
  lazy val orderReadCoordinator = routerFor("order_read_coordinator")
  lazy val orderUpdateCoordinator = routerFor("order_update_coordinator")
  lazy val orderUpdator = routerFor("order_updator")

  lazy val balanceReader = routerFor("balance_reader")
  lazy val orderReader = routerFor("order_reader")
  lazy val orderWriter = routerFor("order_writer")

  lazy val orderAccessor = routerFor("order_accessor")
  lazy val orderDBAccessor = routerFor("order_db_accessor")

  lazy val orderBookManager = routerForSingleton("order_book_manager")
  lazy val ringFinder = routerForSingleton("ring_finder")
  lazy val ringMiner = routerForSingleton("ring_miner")
  lazy val orderBookReader = routerFor("order_book_reader")

  private def routerForSingleton(name: String) = {
    system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/singleton_$name/*",
        settings = ClusterSingletonProxySettings(system)),
      name = s"router_singleton_${name}")

  }

  private def routerFor(name: String) = {
    system.actorOf(
      ClusterRouterGroup(
        RoundRobinGroup(Nil),
        ClusterRouterGroupSettings(
          totalInstances = Int.MaxValue,
          routeesPaths = List(s"/user/role_${name}_*"),
          allowLocalRoutees = true)).props,
      name = s"router_${name}")
  }
}
