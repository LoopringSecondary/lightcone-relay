package com.upblockchain.lightcone.orders

import akka.actor._
import akka.persistence._

case class Cmd(data: String)
case class Evt(data: String)

case class OrdersState(events: List[String] = Nil) {
  def updated(evt: Evt): OrdersState = copy(evt.data :: events)
  def size: Int = events.length
  override def toString: String = events.reverse.toString
}

class MarketManager extends PersistentActor {
  override def persistenceId = "order-manager"

  val snapShotInterval = 10000
  var state = OrdersState()

  def updateState(event: Evt): Unit =
    state = state.updated(event)

  val receiveRecover: Receive = {
    case evt: Evt                                => updateState(evt)
    case SnapshotOffer(_, snapshot: OrdersState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case Cmd(data) =>
      persist(Evt("something")) { event =>
        updateState(event)
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
          saveSnapshot(state)
      }
  }

}
