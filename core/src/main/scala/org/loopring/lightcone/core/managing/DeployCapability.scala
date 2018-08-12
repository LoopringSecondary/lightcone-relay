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

  case class ActorProps(
    isSingleton: Boolean,
    namePrefix: String,
    klass: Class[_]) {

    def props(settingsId: Option[String]) = settingsId match {
      case Some(id) => Props(klass, id)
      case None => Props(klass)
    }
  }

  val actorAttributes: Map[String, ActorProps] = Map(
    "cluster_manager" -> ActorProps(true, "m", classOf[ClusterManager]),
    // Service Actors
    "cache_obsoleter" -> ActorProps(true, "s", classOf[CacheObsoleter]),
    "blockchain_event_extractor" -> ActorProps(true, "s", classOf[BlockchainEventExtractor]),
    "balance_cacher" -> ActorProps(false, "s", classOf[BalanceCacher]),
    "balance_manager" -> ActorProps(false, "s", classOf[BalanceManager]),
    "order_cacher" -> ActorProps(false, "s", classOf[OrderCacher]),
    "order_read_coordinator" -> ActorProps(false, "s", classOf[OrderReadCoordinator]),
    "order_update_coordinator" -> ActorProps(false, "s", classOf[OrderUpdateCoordinator]),
    "order_updator" -> ActorProps(false, "s", classOf[OrderUpdater]),
    "balance_reader" -> ActorProps(false, "s", classOf[BalanceReader]),
    "order_reader" -> ActorProps(false, "s", classOf[OrderReader]),
    "order_writer" -> ActorProps(false, "s", classOf[OrderWriter]),
    "order_accessor" -> ActorProps(false, "s", classOf[OrderAccessor]),
    "order_db_accessor" -> ActorProps(false, "s", classOf[OrderDBAccessor]),
    "order_book_manager" -> ActorProps(true, "s", classOf[OrderBookManager]),
    "ring_finder" -> ActorProps(true, "s", classOf[RingFinder]),
    "ring_miner" -> ActorProps(true, "s", classOf[RingMiner]),
    "order_book_reader" -> ActorProps(false, "s", classOf[OrderBookReader]))

  def deployActorByName(name: String, settingsId: Option[String] = None) = {
    actorAttributes.get(name) match {
      case Some(attributes) => deploy(name, attributes, settingsId)
      case _ => log.error(s"$name is not a valid actor name")
    }
  }
  private def deploy(
    name: String,
    ap: ActorProps,
    settingsId: Option[String]) {
    if (ap.isSingleton) {
      try {
        val actor = system.actorOf(
          ClusterSingletonManager.props(
            singletonProps = ap.props(settingsId),
            terminationMessage = PoisonPill,
            settings = ClusterSingletonManagerSettings(system)),
          name = s"${ap.namePrefix}_${name}_${settingsId.getOrElse("")}_0")
        log.info(s"----> deployed actor ${actor.path} as singleton")
      } catch {
        case e: InvalidActorNameException =>
      }
    } else {
      nextActorId += 1
      val actor = system.actorOf(
        ap.props(settingsId),
        name = s"${ap.namePrefix}_${name}_${settingsId.getOrElse("")}_${nextActorId}")
      log.info(s"----> deployed actor ${actor.path}")
    }
  }
}
