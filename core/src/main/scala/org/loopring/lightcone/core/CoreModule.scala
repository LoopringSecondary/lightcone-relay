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

import com.google.inject._
import net.codingwell.scalaguice._
import com.google.inject.name._
import akka.actor._
import akka.cluster._
import akka.stream.ActorMaterializer
import org.loopring.lightcone.core.actors._
import com.typesafe.config.Config
import akka.util.Timeout
import org.loopring.lightcone.core.accessor.EthClient

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import redis._

class CoreModule(config: Config) extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    implicit val system = ActorSystem("Lightcone", config)
    implicit val cluster = Cluster(system)

    bind[Config].toInstance(config)
    bind[ActorSystem].toInstance(system)
    bind[ExecutionContext].toInstance(system.dispatcher)
    bind[Cluster].toInstance(cluster)
    bind[ActorMaterializer].toInstance(ActorMaterializer())
    bind[Timeout].toInstance(new Timeout(2 seconds))

    bind[RedisCluster].toProvider[cache.RedisClusterProvider].in[Singleton]
  }

  @Provides
  @Singleton
  @Named("node_manager")
  def getNodeManager(injector: Injector, config: Config)(implicit
    cluster: Cluster,
    materializer: ActorMaterializer) = {

    cluster.system.actorOf(
      Props(new managing.NodeManager(injector, config)), "node_manager")
  }

  @Provides
  @Named("balance_cacher")
  def getBalanceCacherProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new BalanceCacher()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("balance_manager")
  def getBalanceManagerProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new BalanceManager()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("balance_reader")
  def getBalanceReaderProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new BalanceReader()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("block_event_extractor")
  def getBlockchainEventExtractorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new BlockchainEventExtractor()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("cache_obsoleter")
  def getCacheObsoleterProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new CacheObsoleter()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("cluster_manager")
  def getClusterManagerProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new ClusterManager()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("ethereum_accessor")
  def getEthereumAccessorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new EthereumAccessor()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_accessor")
  def getOrderAccessorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderAccessor()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_book_manager")
  def getOrderBookManagerProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderBookManager()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_book_reader")
  def getOrderBookReaderProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderBookReader()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_cacher")
  def getOrderCacherProps(redisCluster: RedisCluster)(implicit
    context: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderCacher(redisCluster)) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_change_log_writer")
  def getOrderChangeLogWriterProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderChangeLogWriter()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_db_accessor")
  def getOrderDBAccessorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderDBAccessor()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_manager")
  def getOrderManagerProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderManager()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_read_coordinator")
  def getOrderReadCoordinatorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderReadCoordinator()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_reader")
  def getOrderReaderProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderReader()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_update_coordinator")
  def getOrderUpdateCoordinatorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderUpdateCoordinator()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_updater")
  def getOrderUpdaterProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderUpdater()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("order_writer")
  def getOrderWriterProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderWriter()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("ring_finder")
  def getRingFinderProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new RingFinder()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("ring_miner")
  def getRingMinerProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new RingMiner()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("ring_evaluator")
  def getRingEvaluatorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new RingEvaluator()) // .withDispatcher("ring-dispatcher")
  }

  @Provides
  @Named("ring_submitter")
  def getRingSubmitterProps(@Inject() ethClient: EthClient)(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new RingSubmitter(ethClient)) // .withDispatcher("ring-dispatcher")
  }

}