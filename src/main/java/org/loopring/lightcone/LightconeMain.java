package org.loopring.lightcone;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.loopring.lightcone.core.order.OrderActor;
import org.loopring.lightcone.core.order.TxActor;
import org.loopring.lightcone.proto.Data;

public class LightconeMain {
  public static void main(String[] args) {

    ActorSystem system = ActorSystem.create("lightcone");

    try {
      // Create top level supervisor
      ActorRef orderActor = system.actorOf(OrderActor.props(), "orderActor");
      ActorRef txActor = system.actorOf(TxActor.props(), "txActor");
      System.out.println();
      orderActor.tell(Data.OrderQuery.newBuilder().setOrderHash("aaa"), txActor);

      System.out.println("Press ENTER to exit the system");
      System.in.read();

    } catch (Exception e) {
      System.out.println(e);

    } finally {
      system.terminate();
    }
  }
}
