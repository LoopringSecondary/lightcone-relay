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
import org.loopring.lightcone.proto.deployment._
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
    bind[WethAbi].to[WethAbi].in[Singleton]
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
    bind[BlockHelper].to[BlockHelperImpl].in[Singleton]
    bind[TransactionHelper].to[TransactionHelperImpl].in[Singleton]
  }

  @Provides
  @Singleton
  @Named("node_manager")
  def getNodeManager(config: Config)(implicit
    injector: Injector,
    cluster: Cluster,
    materializer: ActorMaterializer) = {

    cluster.system.actorOf(
      Props(new managing.NodeManager(config)), "node_manager")
  }

  @Provides
  @Named("balance_cacher")
  def getBalanceCacherProps(cache: BalanceCache)(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: BalanceCacherSettings =>
      Props(new BalanceCacher(settings, cache))
  }

  @Provides
  @Named("balance_manager")
  def getBalanceManagerProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: BalanceManagerSettings =>
      Props(new BalanceManager(settings))
  }

  @Provides
  @Named("balance_reader")
  def getBalanceReaderProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: BalanceReaderSettings =>
      Props(new BalanceReader(settings))
  }

  @Provides
  @Named("block_event_extractor")
  def getBlockchainEventExtractorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout,
    blockHelper: BlockHelper,
    transactionHelper: TransactionHelper) = {
    settings: BlockchainEventExtractorSettings =>
      Props(new BlockchainEventExtractor(settings))
  }

  @Provides
  @Named("cache_obsoleter")
  def getCacheObsoleterProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: CacheObsoleterSettings =>
      Props(new CacheObsoleter(settings))
  }

  @Provides
  @Named("cluster_manager")
  def getClusterManagerProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    Props(new ClusterManager())
  }

  @Provides
  @Named("ethereum_accessor")
  def getEthereumAccessorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: EthereumAccessorSettings =>
      Props(new EthereumAccessor(settings))
  }

  @Provides
  @Named("order_accessor")
  def getOrderAccessorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderAccessorSettings =>
      Props(new OrderAccessor(settings))
  }

  @Provides
  @Named("order_book_manager")
  def getOrderBookManagerProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderBookManagerSettings =>
      Props(new OrderBookManager(settings))
  }

  @Provides
  @Named("order_book_reader")
  def getOrderBookReaderProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderBookReaderSettings =>
      Props(new OrderBookReader(settings))
  }

  @Provides
  @Named("order_cacher")
  def getOrderCacherProps(cache: OrderCache)(implicit
    context: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderCacherSettings =>
      Props(new OrderCacher(settings, cache))
  }

  @Provides
  @Named("order_change_log_writer")
  def getOrderChangeLogWriterProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderChangeLogWriterSettings =>
      Props(new OrderChangeLogWriter(settings))
  }

  @Provides
  @Named("order_db_accessor")
  def getOrderDBAccessorProps(db: OrderDatabase)(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderDBAccessorSettings =>
      Props(new OrderDBAccessor(settings, db))
  }

  @Provides
  @Named("order_manager")
  def getOrderManagerProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderManagerSettings =>
      Props(new OrderManager(settings))
  }

  @Provides
  @Named("order_read_coordinator")
  def getOrderReadCoordinatorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderReadCoordinatorSettings =>
      Props(new OrderReadCoordinator(settings))
  }

  @Provides
  @Named("order_reader")
  def getOrderReaderProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderReaderSettings =>
      Props(new OrderReader(settings))
  }

  @Provides
  @Named("order_update_coordinator")
  def getOrderUpdateCoordinatorProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderUpdateCoordinatorSettings =>
      Props(new OrderUpdateCoordinator(settings))
  }

  @Provides
  @Named("order_updater")
  def getOrderUpdaterProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderUpdaterSettings =>
      Props(new OrderUpdater(settings))
  }

  @Provides
  @Named("order_writer")
  def getOrderWriterProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: OrderWriterSettings =>
      Props(new OrderWriter(settings))
  }

  @Provides
  @Named("ring_finder")
  def getRingFinderProps()(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: RingFinderSettings =>
      Props(new RingFinder(settings))
  }

  @Provides
  @Named("ring_miner")
  def getRingMinerProps(ethClient: EthClient)(implicit
    ec: ExecutionContext,
    timeout: Timeout) = {
    settings: RingMinerSettings =>
      Props(new RingMiner(settings, ethClient))
  }

}