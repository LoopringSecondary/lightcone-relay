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

import akka.actor._
import akka.cluster._
import akka.http.scaladsl.model._
import akka.http.scaladsl._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.google.inject._
import com.google.inject.name._
import com.typesafe.config.Config
import net.codingwell.scalaguice._
import org.loopring.lightcone.core.accessor._
import org.loopring.lightcone.core.actors._
import org.loopring.lightcone.core.cache.{ ByteArrayRedisCache, _ }
import org.loopring.lightcone.core.database._
import org.loopring.lightcone.core.utils._
import org.loopring.lightcone.lib.abi._
import org.loopring.lightcone.lib.cache.ByteArrayCache
import org.loopring.lightcone.proto.token.TokenList
import redis._

import scala.concurrent.{ ExecutionContext, _ }
import scala.concurrent.duration._

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
    bind[Erc20Abi].to[Erc20Abi].in[Singleton]
    bind[LoopringAbi].to[LoopringAbi].in[Singleton]
    bind[EthClient].to[EthClientImpl].in[Singleton]

    val httpFlow = Http()
      .cachedHostConnectionPool[Promise[HttpResponse]](
        host = config.getString("ethereum.host"),
        port = config.getInt("ethereum.port"))

    bind[HttpFlow].toInstance(httpFlow)

    bind[Int].annotatedWith(Names.named("ethereum_conn_queuesize"))
      .toInstance(config.getInt("ethereum.queueSize"))

    bind[RedisCluster].toProvider[cache.RedisClusterProvider].in[Singleton]
    bind[OrderDatabase].to[MySQLOrderDatabase]

    bind[ByteArrayCache].to[ByteArrayRedisCache].in[Singleton]
    bind[BalanceCache].to[cache.BalanceRedisCache]
    bind[OrderCache].to[cache.OrderRedisCache]

    bind[TokenList].toInstance(TokenList(list = Seq()))
    bind[ExtractorBlockDetector].to[ExtractorBlockDetectorImpl].in[Singleton]
    bind[ExtractorTransactionProcessor].to[ExtractorTransactionProcessor].in[Singleton]
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
  def getBalanceCacherProps(cache: BalanceCache)(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new BalanceCacher(cache)) // .withDispatcher("ring-dispatcher")
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
    timeout: Timeout,
    detector: ExtractorBlockDetector,
    processor: ExtractorTransactionProcessor) = {
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
  def getOrderCacherProps(cache: OrderCache)(implicit
    context: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderCacher(cache)) // .withDispatcher("ring-dispatcher")
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
  def getOrderDBAccessorProps(db: OrderDatabase)(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new OrderDBAccessor(db)) // .withDispatcher("ring-dispatcher")
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
  def getRingMinerProps(ethClient: EthClient)(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new RingMiner(ethClient)) // .withDispatcher("ring-dispatcher")
  }

}