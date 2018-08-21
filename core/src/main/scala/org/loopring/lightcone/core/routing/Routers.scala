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
import org.loopring.lightcone.core.actors._

object Routers {
  // // Router for management actors
  def clusterManager = routers(ClusterManager.name)("")

  // // Router for service actors
  def cacheObsoleter = routers(CacheObsoleter.name)("")
  def blockchainEventExtractor = routers(BlockchainEventExtractor.name)("")

  def balanceCacher = routers(BalanceCacher.name)("")
  def balanceManager = routers(BalanceManager.name)("")
  def orderCacher = routers(OrderCacher.name)("")
  def orderReadCoordinator = routers(OrderReadCoordinator.name)("")
  def orderUpdateCoordinator = routers(OrderUpdateCoordinator.name)("")
  def orderUpdater = routers(OrderUpdater.name)("")

  def balanceReader = routers(BalanceReader.name)("")
  def orderReader = routers(OrderReader.name)("")
  def orderWriter = routers(OrderWriter.name)("")

  def orderManager = routers(OrderManager.name)("")
  def orderAccessor = routers(OrderAccessor.name)("")
  def orderDBAccessor = routers(OrderDBAccessor.name)("")

  def ringFinder(id: String) = routers(RingFinder.name)(id)
  def orderBookManager(id: String) = routers(OrderBookManager.name)(id)
  def orderBookReader(id: String) = routers(OrderBookReader.name)(id)
  def ringMiner(address: String) = routers(RingMiner.name)(address)

  private var routers: Map[String, Map[String, ActorRef]] = Map.empty

  def setRouters(name: String, routerMap: Map[String, ActorRef]) {
    routers = routers + (name -> routerMap)
  }
}
