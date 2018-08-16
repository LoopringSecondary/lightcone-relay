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
import akka.pattern.Patterns;
import org.loopring.lightcone.proto.cache.CachedMultiOrders;
import org.loopring.lightcone.proto.cache.GetOrdersFromCache;
import org.loopring.lightcone.proto.order.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Optional;

public class OrderManager extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(Optional<String> settingsId) {
        return Props.create(OrderManager.class);
    }

    private ActorRef orderAccessor;
    private ActorRef orderUpdater;
    private ActorRef orderCacher;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetOrder.class, r -> {
                    Future f = Patterns.ask(orderAccessor, r , 1000);
                    OneOrder oneOrder = (OneOrder) Await.result(f, Duration.create(1, "second"));
                    Future ff = Patterns.ask(orderCacher, GetOrdersFromCache.defaultInstance(), 1000);
                    CachedMultiOrders cachedMultiOrders = (CachedMultiOrders) Await.result(ff, Duration.create(1, "second"));
                    // 1. 从db拿到order
                    // 2. 查看缓存，如果没有再从orderUpdater重新计算
                    // 3. 如果存在，直接返回缓存
                    // 4. 重新计算后，再更新缓存
//                    if cachedMultiOrders not exist {
//                        Future fff = Patterns.ask(orderUpdater, CalculateOrdersStatus.defaultInstance() , 1000);
//                        OrdersStatus ordersUpdated = (OrdersStatus) Await.result(fff, Duration.create(1, "second"));
//                        orderCacher.tell(OrderStatusChanged.defaultInstance(), getSelf());
//                    }

                    getSender().tell(OneOrder.defaultInstance(), getSender());
                })
                .match(GetOrders.class, r -> {
                    // same to getOrder
                })
                // 这里包装成UpdateOrders发给OrderUpdater，由它计算最终状态并通知socketio和更新cache
                .match(OrdersSaved.class, r -> orderUpdater.tell(UpdateOrders.defaultInstance(), getSelf()))
                .match(OrdersSoftCancelled.class, r -> orderUpdater.tell(UpdateOrders.defaultInstance(), getSelf()))
                .build();
    }

    private OneOrder convert(OrdersUpdated ordersUpdated) {
        return OneOrder.defaultInstance();
    }

}
