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

trait DeployCapability {
  me: Actor with ActorLogging =>

  implicit val cluster: Cluster
  implicit val system: ActorSystem
  var nextActorId = 0;

  lazy val nameMap: Map[String, (Boolean, String, Props)] = Map(
    // Management Actors
    "cluster_manager" -> (true, "management", Props(classOf[ClusterManager])),
    // Service Actors
    "cache_obsoleter" -> (true, "service", Props(classOf[CacheObsoleter])),
    "blockchain_event_extractor" -> (true, "service", Props(classOf[BlockchainEventExtractor])),
    "balance_cacher" -> (false, "service", Props(classOf[BalanceCacher])),
    "balance_manager" -> (false, "service", Props(classOf[BalanceManager])),
    "order_cacher" -> (false, "service", Props(classOf[OrderCacher])),
    "order_read_coordinator" -> (false, "service", Props(classOf[OrderReadCoordinator])),
    "order_update_coordinator" -> (false, "service", Props(classOf[OrderUpdateCoordinator])),
    "order_updator" -> (false, "service", Props(classOf[OrderUpdater])),
    "balance_reader" -> (false, "service", Props(classOf[BalanceReader])),
    "order_reader" -> (false, "service", Props(classOf[OrderReader])),
    "order_writer" -> (false, "service", Props(classOf[OrderWriter])),
    "order_accessor" -> (false, "service", Props(classOf[OrderAccessor])),
    "order_db_accessor" -> (false, "service", Props(classOf[OrderDBAccessor])),
    "order_book_manager" -> (true, "service", Props(classOf[OrderBookManager])),
    "ring_finder" -> (true, "service", Props(classOf[RingFinder])),
    "ring_miner" -> (true, "service", Props(classOf[RingMiner])),
    "order_book_reader" -> (false, "service", Props(classOf[OrderBookReader])))

  def deployActorByName(name: String) = {
    nameMap.get(name) match {
      case Some((asSingleton, group, props)) => deploy(name, asSingleton, group, props)
      case _ => log.error(s"$name is not a valid actor name")
    }
  }
  private def deploy(
    name: String,
    asSingleton: Boolean,
    group: String,
    props: Props) {
    if (asSingleton) {
      try {
        val actor = system.actorOf(
          ClusterSingletonManager.props(
            singletonProps = props,
            terminationMessage = PoisonPill,
            settings = ClusterSingletonManagerSettings(system)),
          name = s"${group}_${name}_0")
        log.info(s"----> deployed actor ${actor.path} as singleton")
      } catch {
        case e: InvalidActorNameException =>
      }
    } else {
      nextActorId += 1
      val actor = system.actorOf(props, s"${group}_${name}_${nextActorId}")
      log.info(s"----> deployed actor ${actor.path}")
    }
  }
}
