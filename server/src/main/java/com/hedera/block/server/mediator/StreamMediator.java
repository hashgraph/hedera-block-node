/*
 * Hedera Block Node
 *
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

import com.hedera.block.server.consumer.LiveStreamObserver;

/**
 * The StreamMediator interface represents a one-to-many bridge between a bidirectional stream of blocks from a
 * producer (e.g. a Consensus Node) and N consumers each requesting a bidirectional connection to get
 * a "live stream" of blocks from the producer.  StreamMediator satisfies Helidon's type requirements for a
 * bidirectional StreamObserver representing a stream of blocks returned FROM the downstream consuming client.
 * However, the StreamObserver type may be distinct from Block type streamed TO the client.  The type definition
 * for the onNext() method provides the flexibility for the StreamObserver and the Block types to vary independently.
 *
 * @param <U> The type of the block
 * @param <V> The type of the StreamObserver
 */
public interface StreamMediator<U, V> {

    /**
     * Subscribes a new LiveStreamObserver to receive blocks from the producer as they arrive
     *
     * @param observer - the LiveStreamObserver to subscribe
     */
    void subscribe(LiveStreamObserver<U, V> observer);

    /**
     * Unsubscribes a LiveStreamObserver from the producer
     *
     * @param observer - the LiveStreamObserver to unsubscribe
     */
    void unsubscribe(LiveStreamObserver<U, V> observer);

    /**
     * Checks if the observer is subscribed to the producer
     *
     * @param observer - the LiveStreamObserver to check
     * @return true if the observer is subscribed, false otherwise
     */
    boolean isSubscribed(LiveStreamObserver<U, V> observer);

    /**
     * Unsubscribes all LiveStreamObservers from the producer
     */
    void unsubscribeAll();

    /**
     * Passes the newly arrived block to all subscribers
     *
     * @param block - the block to pass to the subscribers
     */
    void notifyAll(U block);
}
