package com.hedera.block.simulator.grpc.impl;

import com.hedera.block.simulator.grpc.PublishStreamGrpcServer;

import javax.inject.Inject;
import java.util.List;

public class PublishStreamGrpcServerImpl implements PublishStreamGrpcServer {

    @Inject
    public PublishStreamGrpcServerImpl() {

    }

    /**
     * Gets the number of processed blocks.
     *
     * @return the number of published blocks
     */
    @Override
    public long getProcessedBlocks() {
        return 0;
    }

    /**
     * Gets the last known statuses.
     *
     * @return the last known statuses
     */
    @Override
    public List<String> getLastKnownStatuses() {
        return null;
    }
}
