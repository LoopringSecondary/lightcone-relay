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

import com.google.inject._
import net.codingwell.scalaguice._
import com.google.inject.name._
import akka.actor._
import akka.cluster._
import akka.stream.ActorMaterializer
import org.loopring.lightcone.core.actors._
import com.typesafe.config.Config

class CoreModule(config: Config) extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    implicit val system = ActorSystem("Lightcone", config)
    implicit val cluster = Cluster(system)
    val materializer = ActorMaterializer()
    val context = system.dispatcher

    bind[Config].toInstance(config)
    bind[ActorSystem].toInstance(system)
    bind[Cluster].toInstance(cluster)
    bind[ActorMaterializer].toInstance(materializer)
  }

  @Provides
  @Singleton
  @Named("node_manager")
  def getNodeManager(config: Config)(implicit
    cluster: Cluster,
    materializer: ActorMaterializer) = {

    cluster.system.actorOf(
      Props(new managing.NodeManager(config)), "node_manager")
  }
}