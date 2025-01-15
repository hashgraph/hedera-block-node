// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.grpc;

import java.util.List;

public interface PublishStreamGrpcServer {
    void init();
    /**
     * Gets the number of processed blocks.
     *
     * @return the number of published blocks
     */
    long getProcessedBlocks();

    /**
     * Gets the last known statuses.
     *
     * @return the last known statuses
     */
    List<String> getLastKnownStatuses();

    /**
     * Sends a onCompleted message to the client and waits for a short period of time to ensure the message is sent.
     */
    void completeStreaming();

    /**
     * Shutdowns the server.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    void shutdown() throws InterruptedException;
}
