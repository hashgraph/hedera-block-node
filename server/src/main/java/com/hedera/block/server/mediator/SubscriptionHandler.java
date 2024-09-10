/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hedera.block.server.mediator;

import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The SubscriptionHandler interface defines the contract for subscribing and unsubscribing
 * downstream consumers to the stream of events.
 *
 * @param <V> the type of the subscription events
 */
public interface SubscriptionHandler<V> {

    /**
     * Subscribes the given handler to the stream of events.
     *
     * @param handler the handler to subscribe
     */
    void subscribe(@NonNull final BlockNodeEventHandler<ObjectEvent<V>> handler);

    /**
     * Unsubscribes the given handler from the stream of events.
     *
     * @param handler the handler to unsubscribe
     */
    void unsubscribe(@NonNull final BlockNodeEventHandler<ObjectEvent<V>> handler);

    /**
     * Checks if the given handler is subscribed to the stream of events.
     *
     * @param handler the handler to check
     * @return true if the handler is subscribed, false otherwise
     */
    boolean isSubscribed(@NonNull final BlockNodeEventHandler<ObjectEvent<V>> handler);

    void unsubscribeAllExpired();
}
