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
import akka.pattern._
import akka.util.Timeout
import org.loopring.lightcone.core.actors.base._
import org.loopring.lightcone.core.routing._
import org.loopring.lightcone.proto.deployment._
import org.loopring.lightcone.proto.ring._

import scala.concurrent.Future

object RingMiner
  extends base.Deployable[RingMinerSettings] {
  val name = "ring_miner"
  override val isSingleton = true

  def getCommon(s: RingMinerSettings) =
    base.CommonSettings(Some(s.address), s.roles, 1)
}

class RingMiner()(implicit val timeout: Timeout)
  extends RepeatedJobActor {

  import context.dispatcher
  var finders: Seq[ActorRef] = Seq.empty

  override def receive: Receive = super.receive orElse {
    case settings: RingMinerSettings =>
      finders = settings.marketIds.map { id => Routers.ringFinder(id) }
      initAndStartNextRound(settings.scheduleDelay)
  }

  def handleRepeatedJob() = for {
    resps <- Future.sequence(finders.map { _ ? GetRingCandidates() })
      .mapTo[Seq[RingCandidates]]
  } yield {
    //    val evaluator = new Evaluator("delegateAddress")
    //
    //    val rings = resps.flatMap(_.rings)
    //    val ringCandidates = rings
    //      .map(r => Some(RingCandidate(rawRing = r)))
    //
    //    val ringForSubmit = getRingForSubmit(Seq(), ringCandidates, evaluator)

  }

}

