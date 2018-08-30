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

import akka.actor.Actor
import akka.util.Timeout
import org.loopring.lightcone.proto.deployment.RingSubmitterSettings

/*

  Copyright 2017 Loopring Project Ltd (Loopring Foundation).

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

*/

object RingSubmitter
  extends base.Deployable[RingSubmitterSettings] {
  val name = "ring_submitter"
  override val isSingleton = true

  def getCommon(s: RingSubmitterSettings) =
    base.CommonSettings(Some(s.id), s.roles, 1)
}

class RingSubmitter()(implicit val timeout: Timeout)
  extends Actor {
  override def receive: Receive = {
    case settings: RingSubmitterSettings =>
    case _ =>
  }
}
