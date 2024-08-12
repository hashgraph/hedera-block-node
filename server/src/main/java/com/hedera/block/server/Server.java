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

package com.hedera.block.server;

import static com.hedera.block.protos.BlockStreamService.*;
import static com.hedera.block.server.Constants.BLOCKNODE_SERVER_CONSUMER_TIMEOUT_THRESHOLD_KEY;
import static com.hedera.block.server.Constants.BLOCKNODE_STORAGE_ROOT_PATH_KEY;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.config.BlockNodeContextFactory;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.LiveStreamMediatorBuilder;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.producer.ItemAckBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.grpc.GrpcRouting;
import java.io.IOException;

/** Main class for the block node server */
public class Server {

    private static final System.Logger LOGGER = System.getLogger(Server.class.getName());

    private Server() {}

    /**
     * Main entrypoint for the block node server
     *
     * @param args Command line arguments. Not used at present.
     */
    public static void main(final String[] args) {

        LOGGER.log(System.Logger.Level.INFO, "Starting BlockNode Server");

        try {
            // init metrics
            @NonNull final BlockNodeContext blockNodeContext = BlockNodeContextFactory.create();

            // increase by 1 just for the sake of an example
            @NonNull final MetricsService metricsService = blockNodeContext.metricsService();
            metricsService.exampleCounter.increment();

            // Set the global configuration
            @NonNull final Config config = Config.create();
            Config.global(config);

            @NonNull final ServiceStatus serviceStatus = new ServiceStatusImpl();

            @NonNull
            final BlockWriter<BlockItem> blockWriter =
                    BlockAsDirWriterBuilder.newBuilder(
                                    BLOCKNODE_STORAGE_ROOT_PATH_KEY, config, blockNodeContext)
                            .build();
            @NonNull
            final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> streamMediator =
                    LiveStreamMediatorBuilder.newBuilder(
                                    blockWriter, blockNodeContext, serviceStatus)
                            .build();

            @NonNull
            final BlockReader<Block> blockReader =
                    BlockAsDirReaderBuilder.newBuilder(BLOCKNODE_STORAGE_ROOT_PATH_KEY, config)
                            .build();

            @NonNull
            final BlockStreamService blockStreamService =
                    buildBlockStreamService(
                            config, streamMediator, blockReader, serviceStatus, blockNodeContext);

            @NonNull
            final GrpcRouting.Builder grpcRouting =
                    GrpcRouting.builder().service(blockStreamService);

            // Build the web server
            @NonNull
            final WebServer webServer =
                    WebServer.builder().port(8080).addRouting(grpcRouting).build();

            // Update the serviceStatus with the web server
            serviceStatus.setWebServer(webServer);

            // Start the web server
            webServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private static BlockStreamService buildBlockStreamService(
            @NonNull final Config config,
            @NonNull
                    final StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>>
                            streamMediator,
            @NonNull final BlockReader<Block> blockReader,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final BlockNodeContext blockNodeContext) {

        // Get Timeout threshold from configuration
        final long consumerTimeoutThreshold =
                config.get(BLOCKNODE_SERVER_CONSUMER_TIMEOUT_THRESHOLD_KEY).asLong().orElse(1500L);

        return new BlockStreamService(
                consumerTimeoutThreshold,
                new ItemAckBuilder(),
                streamMediator,
                blockReader,
                serviceStatus,
                blockNodeContext);
    }
}
