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

package org.loopring.lightcone.core.actors.base

import akka.actor._
import akka.cluster._
import akka.routing._
import akka.cluster.singleton._
import akka.cluster.routing._
import org.loopring.lightcone.core.routing.Routers
import com.typesafe.config.Config
import org.loopring.lightcone.proto.deployment._
import akka.event.Logging
import com.google.inject._
import org.loopring.lightcone.core.ActorUtil._

abstract class Deployable[S] {
  val name: String
  val isSingleton = false
  def props(dynamicSettings: DynamicSettings, settings: S)(
    implicit
    injector: Injector) =
    injector.getPropsForSettings(name)(dynamicSettings, settingsMap)

  def getCommon(s: S): CommonSettings

  private var minor_id = 0
  private def nextId: Int = {
    minor_id += 1
    minor_id
  }

  case class SettingsWrapper[S](
    common: CommonSettings,
    settings: S) {
    def numLocalInstances(implicit cluster: Cluster): Int = {
      val roles = common.roles.toSet
      if (roles.isEmpty) common.instances
      else if (roles.intersect(cluster.selfRoles.toSet).isEmpty) 0
      else common.instances
    }
  }

  private var settingsMap = Map.empty[String, SettingsWrapper[S]]

  private def getActorName(id: String, index: Int) = {
    if (isSingleton) s"${name}_${id}_0"
    else s"${name}_${id}_${index}"
  }

  private def actorSelection(id: String)(implicit cluster: Cluster) = {
    if (isSingleton) cluster.system.actorSelection(s"/user/${name}_${id}_0")
    else cluster.system.actorSelection(s"/user/${name}_${id}_*")
  }

  def deploy(settings: Option[S])(
    implicit
    dynamicSettings: DynamicSettings,
    injector: Injector,
    cluster: Cluster): Map[String, ActorRef] = {
    deploy(settings.toSeq)
  }

  def deploy(settingsSeq: Seq[S])(
    implicit
    dynamicSettings: DynamicSettings,
    injector: Injector,
    cluster: Cluster): Map[String, ActorRef] = {
    val oldSettingsMap = settingsMap

    settingsMap = settingsSeq.map { s =>
      val common = getCommon(s)
      val wrapper = SettingsWrapper(common, s)
      common.id.getOrElse("") -> wrapper
    }.toMap

    val ids = oldSettingsMap.keys ++ settingsMap.keys

    ids.map { id =>
      id -> (oldSettingsMap.get(id), settingsMap.get(id))
    } foreach {
      case (id, (_old, _new)) =>
        deployActor(dynamicSettings, id, _old, _new)
    }

    println(s"--------> killing router: /user/r_${name}_*")
    cluster.system.actorSelection(s"/user/r_${name}_*") ! PoisonPill

    // Deploy routers
    settingsMap.keys.map {
      id =>
        val actor =
          if (isSingleton) {
            cluster.system.actorOf(
              ClusterSingletonProxy.props(
                singletonManagerPath = s"/user/${name}_${id}_0",
                settings = ClusterSingletonProxySettings(cluster.system)),
              name = s"r_${name}_${id}_${nextId}")
          } else {
            cluster.system.actorOf(
              ClusterRouterGroup(
                RoundRobinGroup(Nil),
                ClusterRouterGroupSettings(
                  totalInstances = Int.MaxValue,
                  routeesPaths = List(s"/user/${name}_${id}_*"),
                  allowLocalRoutees = true)).props,
              name = s"r_${name}_${id}_${nextId}")
          }
        println("--------> deployed router: " + actor.path)
        (id -> actor)
    }.toMap
  }

  def deployActor(
    dynamicSettings: DynamicSettings,
    id: String,
    _old: Option[SettingsWrapper[S]],
    _new: Option[SettingsWrapper[S]])(
    implicit
    injector: Injector,
    cluster: Cluster): Unit = {

    def getInstances(w: Option[SettingsWrapper[S]]) = {
      val num = w.map(_.numLocalInstances).getOrElse(0)
      if (isSingleton && num > 0) 1 else num
    }

    val newInstances = getInstances(_new)
    val oldInstances = getInstances(_old)

    val instances = if (newInstances < oldInstances) {
      actorSelection(id) ! PoisonPill
      newInstances
    } else {
      newInstances - oldInstances
    }

    (0 until instances) foreach { i =>
      val name = getActorName(id, nextId)
      val actor =
        if (isSingleton) {
          cluster.system.actorOf(
            ClusterSingletonManager.props(
              singletonProps = props(dynamicSettings, _new.get.settings),
              terminationMessage = PoisonPill,
              settings = ClusterSingletonManagerSettings(cluster.system)),
            name = name)
        } else {
          cluster.system.actorOf(props(dynamicSettings, _new.get.settings), name)
        }
    }
  }

}
