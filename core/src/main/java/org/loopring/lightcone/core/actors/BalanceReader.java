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
import scala.concurrent.Await;
import scala.concurrent.Future;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import org.loopring.lightcone.proto.balance.*;
import scala.concurrent.duration.Duration;

import java.util.Optional;

public class BalanceReader extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(Optional<String> settingsId) {
        return Props.create(BalanceReader.class);
    }

    private ActorRef balanceManager;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(BalancesReq.class, r -> {
                    Future f = Patterns.ask(balanceManager, GetAddressBalanceInfo.defaultInstance(), 1000);
                    AddressBalanceInfo balanceInfo = (AddressBalanceInfo) Await.result(f, Duration.create(1, "second"));
                    getSender().tell(convert(balanceInfo), getSender());
                })
                .match(AllowancesReq.class, r -> getSender().tell(new AllowancesResp(), getSender()))
                .build();
    }

    private BalancesResp convert(AddressBalanceInfo balanceInfo) {
        return BalancesResp.defaultInstance();
    }
}
