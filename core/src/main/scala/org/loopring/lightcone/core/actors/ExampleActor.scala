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

package org.loopring.lightcone.core.actors

import akka.actor._
import akka.cluster._
import akka.routing._
import akka.cluster.routing._
import org.loopring.lightcone.core.routing.Routers
import com.typesafe.config.Config
import org.loopring.lightcone.data.deployment._

case class RolesAndInstances(roles: Seq[String], instances: Int)

trait Deployable[S <: AnyRef] {
  protected var settings: Option[S] = None
  protected def getRolesAndInstances(s: S): RolesAndInstances

  protected def getNumOfInstances(
    settings: Option[S])(implicit cluster: Cluster): Int = {
    settings match {
      case Some(s) =>
        val rolesAndInstances = getRolesAndInstances(s)
        if (rolesAndInstances.roles.toSet.intersect(cluster.selfRoles.toSet).isEmpty) {
          0
        } else {
          rolesAndInstances.instances
        }
      case _ => 0
    }
  }

  val actorName: String
  def props(): Props

  def getSelectionPattern() = s"/user/${actorName}_*"
  def getActorName(id: Int) = s"${actorName}_${id}"

  def actors(implicit cluster: Cluster) = cluster.system.actorSelection(getSelectionPattern)

  def redeploy(settings: Option[S])(implicit cluster: Cluster): Unit = {
    val oldInstances = getNumOfInstances(this.settings)
    val newInstances = getNumOfInstances(settings)
    val (base, instances) = if (newInstances < oldInstances) {
      actors ! PoisonPill
      (0, newInstances)
    } else {
      (oldInstances, newInstances - oldInstances)
    }

    (0 until instances) foreach { i =>
      cluster.system.actorOf(props(), getActorName(base + i))
    }

    this.settings = settings;
    settings.foreach { s => actors ! s }
  }

  def deployRouter(implicit cluster: Cluster): ActorRef = {
    cluster.system.actorOf(
      ClusterRouterGroup(
        RoundRobinGroup(Nil),
        ClusterRouterGroupSettings(
          totalInstances = Int.MaxValue,
          routeesPaths = List(getSelectionPattern),
          allowLocalRoutees = true)).props,
      name = s"r_${actorName}")
  }
}

object ExampleActor extends Deployable[ExampleActorSettings] {
  val actorName = "example"
  def getRolesAndInstances(s: ExampleActorSettings) = RolesAndInstances(s.roles, s.instances)
  def props = Props(classOf[ExampleActor])
}
class ExampleActor() extends Actor {
  def receive: Receive = {
    case settings: ExampleActorSettings =>
    case _ =>
  }
}