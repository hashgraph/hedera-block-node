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

package com.hedera.block.simulator.config;

import com.swirlds.config.extensions.sources.ConfigMapping;
import com.swirlds.config.extensions.sources.MappedConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;

/**
 * A class that extends {@link MappedConfigSource} in order to have project-relevant initialization.
 */
public final class SimulatorMappedConfigSourceInitializer {
    private static final List<ConfigMapping> MAPPINGS = List.of(
            // gRPC configuration
            new ConfigMapping("grpc.serverAddress", "GRPC_SERVER_ADDRESS"),
            new ConfigMapping("grpc.port", "GRPC_PORT"),

            // Block stream configuration
            new ConfigMapping("blockStream.simulatorMode", "BLOCK_STREAM_SIMULATOR_MODE"),
            new ConfigMapping("blockStream.delayBetweenBlockItems", "BLOCK_STREAM_DELAY_BETWEEN_BLOCK_ITEMS"),
            new ConfigMapping("blockStream.maxBlockItemsToStream", "BLOCK_STREAM_MAX_BLOCK_ITEMS_TO_STREAM"),
            new ConfigMapping("blockStream.streamingMode", "BLOCK_STREAM_STREAMING_MODE"),
            new ConfigMapping("blockStream.millisecondsPerBlock", "BLOCK_STREAM_MILLISECONDS_PER_BLOCK"),
            new ConfigMapping("blockStream.blockItemsBatchSize", "BLOCK_STREAM_BLOCK_ITEMS_BATCH_SIZE"),

            // Block generator configuration
            new ConfigMapping("generator.generationMode", "GENERATOR_GENERATION_MODE"),
            new ConfigMapping("generator.folderRootPath", "GENERATOR_FOLDER_ROOT_PATH"),
            new ConfigMapping("generator.managerImplementation", "GENERATOR_MANAGER_IMPLEMENTATION"),
            new ConfigMapping("generator.paddedLength", "GENERATOR_PADDED_LENGTH"),
            new ConfigMapping("generator.fileExtension", "GENERATOR_FILE_EXTENSION"),
            new ConfigMapping("generator.startBlockNumber", "GENERATOR_START_BLOCK_NUMBER"),
            new ConfigMapping("generator.endBlockNumber", "GENERATOR_END_BLOCK_NUMBER"),

            // Prometheus configuration
            new ConfigMapping("prometheus.endpointEnabled", "PROMETHEUS_ENDPOINT_ENABLED"),
            new ConfigMapping("prometheus.endpointPortNumber", "PROMETHEUS_ENDPOINT_PORT_NUMBER"));

    private SimulatorMappedConfigSourceInitializer() {}

    /**
     * This method constructs, initializes and returns a new instance of {@link MappedConfigSource}
     * which internally uses {@link SystemEnvironmentConfigSource} and maps relevant config keys to
     * other keys so that they could be used within the application
     *
     * @return newly constructed fully initialized {@link MappedConfigSource}
     */
    @NonNull
    public static MappedConfigSource getMappedConfigSource() {
        final MappedConfigSource config = new MappedConfigSource(SystemEnvironmentConfigSource.getInstance());
        MAPPINGS.forEach(config::addMapping);
        return config;
    }
}
