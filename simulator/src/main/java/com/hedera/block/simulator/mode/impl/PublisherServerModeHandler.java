// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.mode.impl;

import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.grpc.PublishStreamGrpcServer;
import com.hedera.block.simulator.mode.SimulatorModeHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Inject;

/**
 * The {@code PublisherServerModeHandler} class implements the {@link SimulatorModeHandler} interface
 * and provides the behavior for a mode where simulator is working using PublishBlockStream and acts as a server.
 *
 * <p>This mode handles dual operations in the block streaming process, utilizing the
 * {@link BlockStreamConfig} for configuration settings. It is designed for scenarios where
 * the simulator needs to handle both the consumption and publication of blocks in parallel.
 *
 * <p>For now, the actual start behavior is not implemented, as indicated by the
 * {@link UnsupportedOperationException}.
 */
public class PublisherServerModeHandler implements SimulatorModeHandler {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // Service dependencies
    private final PublishStreamGrpcServer publishStreamGrpcServer;
    /**
     * Constructs a new {@code PublisherServerModeHandler} with the specified configuration.
     */
    @Inject
    public PublisherServerModeHandler(@NonNull final PublishStreamGrpcServer publishStreamGrpcServer) {
        this.publishStreamGrpcServer = requireNonNull(publishStreamGrpcServer);
    }

    @Override
    public void init() {
        publishStreamGrpcServer.init();
        LOGGER.log(INFO, "gRPC Server initialized for receiving blocks using publish protocol.");
    }

    @Override
    public void start() {}

    /**
     * Gracefully stops both consumption and publishing of blocks.
     *
     * @throws UnsupportedOperationException as this functionality is not yet implemented
     */
    @Override
    public void stop() throws InterruptedException {
        publishStreamGrpcServer.shutdown();
    }
}
