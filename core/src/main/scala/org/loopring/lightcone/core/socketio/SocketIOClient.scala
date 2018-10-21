package org.loopring.lightcone.core.socketio

object SocketIOClient {

  private val subscribers = new java.util.HashSet[SubscriberEvent]

  def add(client: IOClient, event: String, json: String): Unit = {
    subscribers.add(SubscriberEvent(client, event, json))
  }

  def getClients(fallback: String ⇒ Boolean): Seq[SubscriberEvent] = {
    import scala.collection.JavaConverters._
    subscribers.asScala.toSeq.filter(x ⇒ fallback(x.event))
  }

}
