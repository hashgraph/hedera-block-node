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

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.consumer.LiveStreamObserver;
import com.hedera.block.server.persistence.BlockPersistenceHandler;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * LiveStreamMediatorImpl is the implementation of the StreamMediator interface.  It is responsible for
 * managing the subscribe and unsubscribe operations of downstream consumers.  It also proxies live
 * blocks to the subscribers as they arrive and persists the blocks to the block persistence store.
 */
public class LiveStreamMediatorImpl implements StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final Set<LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse>> subscribers = Collections.synchronizedSet(new LinkedHashSet<>());

    private final BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler;

    /**
     * Constructor for the LiveStreamMediatorImpl class.
     *
     * @param blockPersistenceHandler the block persistence handler
     */
    public LiveStreamMediatorImpl(final BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler) {
        this.blockPersistenceHandler = blockPersistenceHandler;
    }

    /**
     * Subscribe a new observer to the mediator
     *
     * @param liveStreamObserver the observer to be subscribed
     */
    @Override
    public void subscribe(final LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver) {
        subscribers.add(liveStreamObserver);
    }

    /**
     * Unsubscribe an observer from the mediator
     *
     * @param liveStreamObserver the observer to be unsubscribed
     */
    @Override
    public void unsubscribe(final LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver) {
        if (subscribers.remove(liveStreamObserver)) {
            LOGGER.log(System.Logger.Level.DEBUG, "Successfully removed observer from subscription list");
        }
    }

    /**
     * Check if an observer is subscribed to the mediator
     *
     * @param observer the observer to be checked
     * @return true if the observer is subscribed, false otherwise
     */
    @Override
    public boolean isSubscribed(final LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> observer) {
        return subscribers.contains(observer);
    }

    /**
     * Notify all observers of a new block
     *
     * @param block the block to be notified to all observers
     */
    @Override
    public void notifyAll(final BlockStreamServiceGrpcProto.Block block) {

        LOGGER.log(System.Logger.Level.DEBUG, "Notifying " + subscribers.size() + " observers of a new block");

        // Proxy the block to all live stream subscribers
        for (final var subscriber : subscribers) {
            subscriber.notify(block);
        }

        // Persist the block
        blockPersistenceHandler.persist(block);
    }
}
