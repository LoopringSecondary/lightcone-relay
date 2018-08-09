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
import org.loopring.lightcone.proto.OrderQuery;
import org.loopring.lightcone.proto.OrderResult;

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
