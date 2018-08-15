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
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.loopring.lightcone.core.routing.Routers;
import org.loopring.lightcone.proto.BlockChainEvent;
import org.loopring.lightcone.proto.Cache;
import org.loopring.lightcone.proto.Order;

import java.util.Optional;

public class CacheObsoleter extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(Optional<String> settingsId) {
        return Props.create(CacheObsoleter.class);
    }

    private ActorRef orderCacher;
    private ActorRef orderAccessor;
    private ActorRef balanceCacher;

    @Override
    public Receive createReceive() {
        //TODO 这里要不要细化到分开balance和allowance变动的事件？
        return receiveBuilder()
                .match(BlockChainEvent.AddressBalanceChanged.class, r -> balanceCacher.tell(Cache.PurgeTokenBalancesForAddresses.getDefaultInstance(), getSelf()))
                .match(BlockChainEvent.OrderCancelledByContract.class, r -> orderCacher.tell(Order.OrderStatusChanged.getDefaultInstance(), getSelf()))
                .match(BlockChainEvent.RingDetectedInMemPoll.class, r -> orderCacher.tell(Order.OrderStatusChanged.getDefaultInstance(), getSelf()))
                .match(BlockChainEvent.RingMined.class, r -> {
                    // for related two order balance changed
                    balanceCacher.tell(Cache.PurgeTokenBalancesForAddresses.getDefaultInstance(), getSelf());
                    balanceCacher.tell(Cache.PurgeTokenBalancesForAddresses.getDefaultInstance(), getSelf());
                    orderCacher.tell(Order.OrderStatusChanged.getDefaultInstance(), getSelf());
                })
                .match(BlockChainEvent.HeartBeat.class, r -> rollbackAll())
                .match(BlockChainEvent.ChainRolledBack.class, r -> rollbackAll())
                .build();
    }

    private void rollbackAll() {
        orderCacher.tell(Cache.PurgeEverything.getDefaultInstance(), getSelf());
        balanceCacher.tell(Cache.PurgeEverything.getDefaultInstance(), getSelf());
    }
}
