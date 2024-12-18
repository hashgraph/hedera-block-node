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

package com.hedera.block.simulator.grpc;

import com.hedera.hapi.block.stream.protoc.Block;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import java.util.List;

/**
 * The PublishStreamGrpcClient interface provides the methods to stream the block and block item.
 */
public interface PublishStreamGrpcClient {
    /**
     * Initialize, opens a gRPC channel and creates the needed stubs with the passed configuration.
     */
    void init();

    /**
     * Streams the block item.
     *
     * @param blockItems list of the block item to be streamed
     * @return true if the block item is streamed successfully, false otherwise
     */
    boolean streamBlockItem(List<BlockItem> blockItems);

    /**
     * Streams the block.
     *
     * @param block the block to be streamed
     * @return true if the block is streamed successfully, false otherwise
     */
    boolean streamBlock(Block block);

    /**
     * Sends a onCompleted message to the server and waits for a short period of time to ensure the message is sent.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    void completeStreaming() throws InterruptedException;

    /**
     * Gets the number of published blocks.
     *
     * @return the number of published blocks
     */
    long getPublishedBlocks();

    /**
     * Gets the last known statuses.
     *
     * @return the last known statuses
     */
    List<String> getLastKnownStatuses();

    /**
     * Shutdowns the channel.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    void shutdown() throws InterruptedException;
}
