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
  var nextActorId = 0;

  lazy val names: Map[String, (Boolean, Props)] = Map(
    "cluster_manager" -> (true, Props(classOf[ClusterManager], routers, config)),
    "cache_obsoleter" -> (true, Props(classOf[CacheObsoleter], routers, config)),
    "blockchain_event_extractor" -> (true, Props(classOf[BlockchainEventExtractor], routers, config)),
    "balance_cacher" -> (false, Props(classOf[BalanceCacher], routers, config)),
    "balance_manager" -> (false, Props(classOf[BalanceManager], routers, config)),
    "order_cacher" -> (false, Props(classOf[OrderCacher], routers, config)),
    "order_read_coordinator" -> (false, Props(classOf[OrderReadCoordinator], routers, config)),
    "order_update_coordinator" -> (false, Props(classOf[OrderUpdateCoordinator], routers, config)),
    "order_updator" -> (false, Props(classOf[OrderUpdater], routers, config)),
    "balance_reader" -> (false, Props(classOf[BalanceReader], routers, config)),
    "order_reader" -> (false, Props(classOf[OrderReader], routers, config)),
    "order_writer" -> (false, Props(classOf[OrderWriter], routers, config)),
    "order_accessor" -> (false, Props(classOf[OrderAccessor], routers, config)),
    "order_db_accessor" -> (false, Props(classOf[OrderDBAccessor], routers, config)),
    "order_book_manager" -> (true, Props(classOf[OrderBookManager], routers, config)),
    "ring_finder" -> (true, Props(classOf[RingFinder], routers, config)),
    "ring_miner" -> (true, Props(classOf[RingMiner], routers, config)),
    "order_book_reader" -> (false, Props(classOf[OrderBookReader], routers, config)))

  def deployActorByName(name: String) = {
    names.get(name) match {
      case Some((asSingleton, props)) => deploy(name, asSingleton, props)
      case _ => log.error(s"$name is not a valid actor name")
    }
  }
  private def deploy(
    name: String,
    asSingleton: Boolean,
    props: Props) {
    if (asSingleton) {
      val actor = system.actorOf(
        ClusterSingletonManager.props(
          singletonProps = props,
          terminationMessage = PoisonPill,
          settings = ClusterSingletonManagerSettings(system)),
        name = "singleton_" + name)
      log.info(s"----> deployed actor ${actor.path} as singleton")
    } else {
      nextActorId += 1
      val name_ = "role_" + name + "_" + nextActorId
      val actor = system.actorOf(props, name_)
      log.info(s"----> deployed actor ${actor.path}")
    }
  }
}
