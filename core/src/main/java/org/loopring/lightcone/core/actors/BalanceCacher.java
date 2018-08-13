package org.loopring.lightcone.core.actors;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class BalanceCacher extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {
        return Props.create(BalanceCacher.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .build();
    }
}
