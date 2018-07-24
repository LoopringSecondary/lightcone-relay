package com.upblockchain.lightcone.core.order;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.upblockchain.lightcone.proto.Data.OrderResult;

public class TxActor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  public static Props props() {
    return Props.create(TxActor.class);
  }

  @Override
  public void preStart() {
    log.info("tx Application started");
  }

  @Override
  public void postStop() {
    log.info("tx Application stopped");
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder().match(OrderResult.class, r -> System.out.println(r)).build();
  }
}
