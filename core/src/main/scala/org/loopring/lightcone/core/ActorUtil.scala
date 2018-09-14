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

object ActorUtil {
  implicit class ActorInjector(injector: Injector) {
    def getActor(name: String): ActorRef = {
      injector.getInstance(Key.get(classOf[ActorRef], Names.named(name)))
    }

    def getProps(name: String): Props = {
      injector.getInstance(Key.get(classOf[Props], Names.named(name)))
    }
  }
}
