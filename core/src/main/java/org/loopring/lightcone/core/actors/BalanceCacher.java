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

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.loopring.lightcone.proto.Balance;
import org.loopring.lightcone.proto.Cache;
import org.loopring.lightcone.proto.Cache.CachedAddressBalanceInfo;
import org.loopring.lightcone.proto.Cache.CacheAddressBalanceInfo;

import java.util.Optional;

public class BalanceCacher extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(Optional<String> settingsId) {
        return Props.create(BalanceCacher.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CacheAddressBalanceInfo.class, r -> getSender().tell(CachedAddressBalanceInfo.getDefaultInstance(), getSender()))
                .match(Cache.GetAddressBalanceFromCache.class, r -> getSender().tell(Balance.AddressBalanceInfo.getDefaultInstance(), getSender()))
                .build();
    }
}
