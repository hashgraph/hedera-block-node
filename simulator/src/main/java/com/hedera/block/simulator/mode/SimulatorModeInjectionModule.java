// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.mode;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.types.SimulatorMode;
import com.hedera.block.simulator.generator.BlockStreamManager;
import com.hedera.block.simulator.grpc.ConsumerStreamGrpcClient;
import com.hedera.block.simulator.grpc.PublishStreamGrpcClient;
import com.hedera.block.simulator.grpc.PublishStreamGrpcServer;
import com.hedera.block.simulator.metrics.MetricsService;
import com.hedera.block.simulator.mode.impl.ConsumerModeHandler;
import com.hedera.block.simulator.mode.impl.PublisherClientModeHandler;
import com.hedera.block.simulator.mode.impl.PublisherServerModeHandler;
import com.swirlds.config.api.Configuration;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Singleton;

@Module
public interface SimulatorModeInjectionModule {

    @Singleton
    @Provides
    static SimulatorModeHandler providesSimulatorModeHandler(
            @NonNull Configuration configuration,
            @NonNull BlockStreamManager blockStreamManager,
            @NonNull PublishStreamGrpcClient publishStreamGrpcClient,
            @NonNull PublishStreamGrpcServer publishStreamGrpcServer,
            @NonNull ConsumerStreamGrpcClient consumerStreamGrpcClient,
            @NonNull MetricsService metricsService) {

        final BlockStreamConfig blockStreamConfig = configuration.getConfigData(BlockStreamConfig.class);
        final SimulatorMode simulatorMode = blockStreamConfig.simulatorMode();
        return switch (simulatorMode) {
            case PUBLISHER_CLIENT -> new PublisherClientModeHandler(
                    blockStreamConfig, publishStreamGrpcClient, blockStreamManager, metricsService);
            case PUBLISHER_SERVER -> new PublisherServerModeHandler(publishStreamGrpcServer);
            case CONSUMER -> new ConsumerModeHandler(consumerStreamGrpcClient);
        };
    }
}
