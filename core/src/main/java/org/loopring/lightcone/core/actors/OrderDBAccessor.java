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

package org.loopring.lightcone.core.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import org.loopring.lightcone.proto.BlockChainEvent;
import org.loopring.lightcone.proto.block_chain_event.ChainRolledBack;
import org.loopring.lightcone.proto.order.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Optional;

public class OrderDBAccessor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(Optional<String> settingsId) {
        return Props.create(OrderDBAccessor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SaveOrders.class, r -> getSender().tell(OrdersSaved.defaultInstance(), getSender()))
                .match(SoftCancelOrders.class, r -> getSender().tell(OrdersSoftCancelled.defaultInstance(), getSender()))
                .match(GetOrder.class, r -> getSender().tell(OneOrder.defaultInstance(), getSender()))
                .match(GetOrders.class, r -> getSender().tell(MultiOrders.defaultInstance(), getSender()))
                .match(GetTopOrders.class, r -> getSender().tell(TopOrders.defaultInstance(), getSender()))
                .match(FetchTopOrderIds.class, r -> getSender().tell(TopOrdersIds.defaultInstance(), getSender()))
                .match(ChainRolledBack.class, r -> {})

                .build();
    }
}
