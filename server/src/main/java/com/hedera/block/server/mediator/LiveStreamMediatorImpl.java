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

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.consumer.LiveStreamObserver;
import com.hedera.block.server.persistence.BlockPersistenceHandler;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * LiveStreamMediatorImpl is the implementation of the StreamMediator interface.  It is responsible for managing
 * the subscription and unsubscription operations of downstream consumers.  It also proxies new blocks
 * to the subscribers as they arrive and persists the blocks to the block persistence store.
 *
 */
public class LiveStreamMediatorImpl implements StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> {

    private final Logger LOGGER = Logger.getLogger(getClass().getName());
    private final Set<LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse>> subscribers = Collections.synchronizedSet(new LinkedHashSet<>());

    private final BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler;

    /**
     * Constructor for the LiveStreamMediatorImpl class.
     *
     * @param blockPersistenceHandler the block persistence handler
     *
     */
    public LiveStreamMediatorImpl(BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler) {
        this.blockPersistenceHandler = blockPersistenceHandler;
    }

    /**
     * Subscribe a new observer to the mediator
     *
     * @param liveStreamObserver - the observer to be subscribed
     */
    @Override
    public void subscribe(LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver) {
        this.subscribers.add(liveStreamObserver);
    }

    /**
     * Unsubscribe an observer from the mediator
     *
     * @param liveStreamObserver - the observer to be unsubscribed
     */
    @Override
    public void unsubscribe(LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver) {
        if (this.subscribers.remove(liveStreamObserver)) {
            LOGGER.finer("Successfully removed observer from subscription list");
        } else {
            LOGGER.finer("Failed to remove observer from subscription list");
        }
    }

    /**
     * Check if an observer is subscribed to the mediator
     *
     * @param observer - the observer to be checked
     * @return true if the observer is subscribed, false otherwise
     */
    @Override
    public boolean isSubscribed(LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> observer) {
        return this.subscribers.contains(observer);
    }

    /**
     * Unsubscribe all observers from the mediator
     */
    @Override
    public void unsubscribeAll() {
        this.subscribers.clear();
    }

    /**
     * Notify all observers of a new block
     *
     * @param block - the block to be notified to all observers
     */
    @Override
    public void notifyAll(BlockStreamServiceGrpcProto.Block block) {

        // Proxy the block to all live stream subscribers
        for (LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> subscriber : subscribers) {
            subscriber.notify(block);
        }

        // Persist the block
        blockPersistenceHandler.persist(block);
    }
}
