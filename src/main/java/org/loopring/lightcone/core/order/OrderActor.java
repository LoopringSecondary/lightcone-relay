package org.loopring.lightcone.core.order;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.loopring.lightcone.proto.Data.OrderQuery;
import org.loopring.lightcone.proto.Data.OrderResult;

public class OrderActor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  public static Props props() {
    return Props.create(OrderActor.class);
  }

  @Override
  public void preStart() {
    log.info("Order Application started");
  }

  @Override
  public void postStop() {
    log.info("Order Application stopped");
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            OrderQuery.class,
            r ->
                getSender()
                    .tell(
                        OrderResult.newBuilder().setOrderHash("aaa").setValidUntil(123232L).build(),
                        getSelf()))
        .build();
  }
}
