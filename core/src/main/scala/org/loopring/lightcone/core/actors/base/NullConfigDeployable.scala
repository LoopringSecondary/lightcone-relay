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
import akka.cluster.routing._
import org.loopring.lightcone.core.routing.Routers
import com.typesafe.config.Config
import org.loopring.lightcone.data.deployment._

case class NullConfig()

abstract class NullConfigDeployable extends Deployable[NullConfig] {
  def getCommon(s: NullConfig): CommonSettings = getCommon()
  def getCommon() = CommonSettings("", Seq.empty, 1)

  def deploy()(implicit cluster: Cluster): Map[String, ActorRef] =
    deploy(Seq(NullConfig()))
}

