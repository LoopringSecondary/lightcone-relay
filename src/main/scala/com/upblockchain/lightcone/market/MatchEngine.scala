package com.upblockchain.lightcone.orders

import akka.actor._
import akka.persistence._

abstract class MatchEngine extends PersistentActor {
  override def persistenceId = "match-engine-"
}
