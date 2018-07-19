// package com.upblockchain.lightcone

// case object Increment
// case object Decrement
// final case class Get(counterId: Long)
// final case class EntityEnvelope(id: Long, payload: Any)

// case object Stop
// final case class CounterChanged(delta: Int)

// class Counter extends PersistentActor {
//   import ShardRegion.Passivate

//   context.setReceiveTimeout(120.seconds)

//   // self.path.name is the entity identifier (utf-8 URL-encoded)
//   override def persistenceId: String = "Counter-" + self.path.name

//   var count = 0

//   def updateState(event: CounterChanged): Unit =
//     count += event.delta

//   override def receiveRecover: Receive = {
//     case evt: CounterChanged => updateState(evt)
//   }

//   override def receiveCommand: Receive = {
//     case Increment => persist(CounterChanged(+1))(updateState)
//     case Decrement => persist(CounterChanged(-1))(updateState)
//     case Get(_) => sender() ! count
//     case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
//     case Stop => context.stop(self)
//   }
// }
