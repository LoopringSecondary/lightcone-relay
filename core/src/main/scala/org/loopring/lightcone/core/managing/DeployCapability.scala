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
    props: Option[String] => Props)

  val actorAttributes: Map[String, ActorProps] = Map(
    // Management Actors
    "cluster_manager" -> ActorProps(true, "m", ClusterManager.props),
    // Service Actors
    "cache_obsoleter" -> ActorProps(true, "s", CacheObsoleter.props),
    "blockchain_event_extractor" -> ActorProps(true, "s", BlockchainEventExtractor.props),
    "balance_cacher" -> ActorProps(false, "s", BalanceCacher.props),
    "balance_manager" -> ActorProps(false, "s", BalanceManager.props),
    "order_cacher" -> ActorProps(false, "s", OrderCacher.props),
    "order_read_coordinator" -> ActorProps(false, "s", OrderReadCoordinator.props),
    "order_update_coordinator" -> ActorProps(false, "s", OrderUpdateCoordinator.props),
    "order_updator" -> ActorProps(false, "s", OrderUpdater.props),
    "balance_reader" -> ActorProps(false, "s", BalanceReader.props),
    "order_reader" -> ActorProps(false, "s", OrderReader.props),
    "order_writer" -> ActorProps(false, "s", OrderWriter.props),
    "order_accessor" -> ActorProps(false, "s", OrderAccessor.props),
    "order_db_accessor" -> ActorProps(false, "s", OrderDBAccessor.props),
    "order_book_manager" -> ActorProps(true, "s", OrderBookManager.props),
    "ring_finder" -> ActorProps(true, "s", RingFinder.props),
    "ring_miner" -> ActorProps(true, "s", RingMiner.props),
    "order_book_reader" -> ActorProps(false, "s", OrderBookReader.props))

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
          name = s"${ap.namePrefix}_${name}_${settingsId.getOrElse("")}")
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
