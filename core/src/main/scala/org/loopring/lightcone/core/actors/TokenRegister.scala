/*
 * Copyright 2018 lightcore-relay
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
import org.loopring.lightcone.proto.deployment._

// todo: 如何实现系统代币的自动添加/删除,重启并生成相关actor(如miner,orderbook...),在哪获取代币列表
object TokenRegister
  extends base.Deployable[TokenRegistrySettings] {
  val name = "token_register"
  override val isSingleton = true

  def props = Props(classOf[TokenRegister])

  def getCommon(s: TokenRegistrySettings) =
    base.CommonSettings(Some(""), Seq.empty, 0)
}

class TokenRegister() extends Actor {
  def receive: Receive = {
    case settings: TokenRegistrySettings ⇒
    case _ ⇒
  }
}
