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
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.core.actors._

object Routers extends RouterMap {
  // // Router for management actors
  def clusterManager = getRouterNamed(ClusterManager.name)

  // // Router for service actors
  def ethereumAccessor = getRouterNamed(EthereumAccessor.name)
  def cacheObsoleter = getRouterNamed(CacheObsoleter.name)
  def blockchainEventExtractor = getRouterNamed(BlockchainEventExtractor.name)

  def balanceCacher = getRouterNamed(BalanceCacher.name)
  def balanceManager = getRouterNamed(BalanceManager.name)
  def orderCacher = getRouterNamed(OrderCacher.name)
  def orderReadCoordinator = getRouterNamed(OrderReadCoordinator.name)
  def orderUpdateCoordinator = getRouterNamed(OrderUpdateCoordinator.name)
  def orderUpdater = getRouterNamed(OrderUpdater.name)

  def balanceReader = getRouterNamed(BalanceReader.name)
  def orderReader = getRouterNamed(OrderReader.name)
  def orderWriter = getRouterNamed(OrderWriter.name)

  def orderAccessor = getRouterNamed(OrderAccessor.name)
  def orderDBAccessor = getRouterNamed(OrderDBAccessor.name)
  def orderManager = getRouterNamed(OrderManager.name)

  def ringFinder(id: String) = getRouterNamed(RingFinder.name, id)
  def orderBookManager(id: String) = getRouterNamed(OrderBookManager.name, id)
  def orderBookReader(id: String) = getRouterNamed(OrderBookReader.name, id)
  def ringMiner(address: String) = getRouterNamed(RingMiner.name, address)
  def ringEvaluator = getRouterNamed(RingEvaluator.name)
  def ringSubmitter = getRouterNamed(RingSubmitter.name)
}
