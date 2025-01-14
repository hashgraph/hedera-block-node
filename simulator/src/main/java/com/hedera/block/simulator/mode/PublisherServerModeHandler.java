// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.mode;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.grpc.PublishStreamGrpcServer;
import edu.umd.cs.findbugs.annotations.NonNull;

import javax.inject.Inject;

import static java.util.Objects.requireNonNull;

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

    /**
     * Initializes resources for both consuming and publishing blocks.
     *
     * @throws UnsupportedOperationException as this functionality is not yet implemented
     */
    @Override
    public void init() {
        throw new UnsupportedOperationException();
    }

    /**
     * Starts both consuming and publishing blocks simultaneously.
     *
     * @throws UnsupportedOperationException as this functionality is not yet implemented
     */
    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gracefully stops both consumption and publishing of blocks.
     *
     * @throws UnsupportedOperationException as this functionality is not yet implemented
     */
    @Override
    public void stop() {
        throw new UnsupportedOperationException();
    }
}
