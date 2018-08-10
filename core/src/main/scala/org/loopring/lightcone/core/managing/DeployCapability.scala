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

package org.loopring.lightcone.core.managing

import scala.collection.mutable.ListBuffer

import akka.actor._
import akka.cluster.singleton._
import akka.event.Logging
import akka.cluster._
import com.typesafe.config.Config
import scala.concurrent.duration._
import org.loopring.lightcone.proto.data._
import org.loopring.lightcone.core.actors._
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.core.managing._

trait DeployCapability {
  me: Actor with ActorLogging =>

  val config: Config
  val routers: Routers

  implicit val cluster: Cluster
  implicit val system: ActorSystem

  lazy val mapping: Map[String, (Boolean, Props)] = Map(
    "cache_obsoleter" -> (true, Props(classOf[CacheObsoleter], routers)),
    "blockchain_event_extractor" -> (true, Props(classOf[BlockchainEventExtractor], routers)),
    "balance_cacher" -> (false, Props(classOf[BalanceCacher], routers)),
    "balance_manager" -> (false, Props(classOf[BalanceManager], routers)),
    "order_cacher" -> (false, Props(classOf[OrderCacher], routers)),
    "order_read_coordinator" -> (false, Props(classOf[OrderReadCoordinator], routers)),
    "order_update_coordinator" -> (false, Props(classOf[OrderUpdateCoordinator], routers)),
    "order_updator" -> (false, Props(classOf[OrderUpdater], routers)),
    "balance_reader" -> (false, Props(classOf[BalanceReader], routers)),
    "order_reader" -> (false, Props(classOf[OrderReader], routers)),
    "order_writer" -> (false, Props(classOf[OrderWriter], routers)),
    "order_accessor" -> (false, Props(classOf[OrderAccessor], routers)),
    "order_db_accessor" -> (false, Props(classOf[OrderDBAccessor], routers)),
    "order_book_manager" -> (true, Props(classOf[OrderBookManager], routers)),
    "ring_finder" -> (true, Props(classOf[RingFinder], routers)),
    "ring_miner" -> (true, Props(classOf[RingMiner], routers)),
    "order_book_reader" -> (false, Props(classOf[OrderBookReader], routers)))

  private def deploy(
    asSingleton: Boolean,
    props: => Props)(implicit ad: ActorDeployment) = {
    if (asSingleton) {
      val actor = system.actorOf(
        ClusterSingletonManager.props(
          singletonProps = props,
          terminationMessage = PoisonPill,
          settings = ClusterSingletonManagerSettings(system)),
        name = "singleton_" + ad.name)
      log.info(s"----> deployed actor ${actor.path} as singleton")
    } else {
      (0 until ad.nrInstances) foreach { i =>
        val name = "role_" + ad.name + "_" + scala.util.Random.nextInt(100000)
        val actor = system.actorOf(props, name)
        log.info(s"----> deployed actor ${actor.path}")
      }
    }
  }
}
