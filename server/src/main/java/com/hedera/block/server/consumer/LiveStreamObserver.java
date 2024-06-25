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

package com.hedera.block.server.consumer;

import io.grpc.stub.StreamObserver;

/**
 * The LiveStreamObserver interface augments the StreamObserver interface with the notify() method thereby
 * allowing a caller to pass a block to the observer of a different type than the StreamObserver.  In this way,
 * the implementation of this interface can receive and process inbound messages with different types from
 * the producer and response messages from the consumer.
 *
 * @param <U> - the type of the block
 * @param <V> - the type of the StreamObserver
 */
public interface LiveStreamObserver<U, V> extends StreamObserver<V> {

    /**
     * Pass the block to the observer.
     *
     * @param block - the block to be passed to the observer
     */
    void notify(final U block);
}
