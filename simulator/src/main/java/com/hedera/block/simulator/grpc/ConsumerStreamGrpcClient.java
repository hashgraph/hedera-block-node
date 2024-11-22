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

import java.util.List;

/**
 * Interface defining the contract for a gRPC client that consumes blocks from a stream.
 * This interface provides methods for initializing, managing, and monitoring block consumption.
 */
public interface ConsumerStreamGrpcClient {

    /**
     * Initializes the gRPC channel and creates the necessary stubs based on configuration.
     * This method must be called before any streaming operations can begin.
     */
    void init();

    /**
     * Requests a stream of blocks from the server within the specified range.
     *
     * @param startBlock The block number to start streaming from (inclusive)
     * @param endBlock The block number to end streaming at (inclusive). Use 0 for infinite streaming
     * @throws InterruptedException if the streaming process is interrupted
     */
    void requestBlocks(long startBlock, long endBlock) throws InterruptedException;

    /**
     * Signals completion of the streaming process to the server.
     * This method should be called to gracefully terminate the stream.
     *
     * @throws InterruptedException if the completion process is interrupted
     */
    void completeStreaming() throws InterruptedException;

    /**
     * Retrieves the total number of blocks that have been consumed from the stream.
     *
     * @return the count of consumed blocks
     */
    long getConsumedBlocks();

    /**
     * Retrieves the most recent status messages received from the server.
     *
     * @return a list of status messages in chronological order
     */
    List<String> getLastKnownStatuses();

    /**
     * Shuts down the gRPC channel and releases associated resources.
     * This method should be called when the client is no longer needed.
     */
    void shutdown();
}
