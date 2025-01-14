package com.hedera.block.simulator.grpc;

import java.util.List;

public interface PublishStreamGrpcServer {
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
}
