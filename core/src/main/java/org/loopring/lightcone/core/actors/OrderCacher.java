package org.loopring.lightcone.core.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class OrderCacher extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {
        return Props.create(OrderCacher.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .build();
    }
}
