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

package com.hedera.block.server.config;

import com.swirlds.config.extensions.sources.ConfigMapping;
import com.swirlds.config.extensions.sources.MappedConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;

/**
 * A class that extends {@link MappedConfigSource} ir order to have project-relevant initialization.
 */
public final class ServerMappedConfigSourceInitializer {
    private static final List<ConfigMapping> MAPPINGS = List.of(
            new ConfigMapping("consumer.timeoutThresholdMillis", "CONSUMER_TIMEOUT_THRESHOLD_MILLIS"),
            new ConfigMapping("persistence.storage.liveRootPath", "PERSISTENCE_STORAGE_LIVE_ROOT_PATH"),
            new ConfigMapping("persistence.storage.archiveRootPath", "PERSISTENCE_STORAGE_ARCHIVE_ROOT_PATH"),
            new ConfigMapping("persistence.storage.type", "PERSISTENCE_STORAGE_TYPE"),
            new ConfigMapping("persistence.storage.compression", "PERSISTENCE_STORAGE_COMPRESSION"),
            new ConfigMapping("persistence.storage.compressionLevel", "PERSISTENCE_STORAGE_COMPRESSION_LEVEL"),
            new ConfigMapping("service.delayMillis", "SERVICE_DELAY_MILLIS"),
            new ConfigMapping("mediator.ringBufferSize", "MEDIATOR_RING_BUFFER_SIZE"),
            new ConfigMapping("mediator.type", "MEDIATOR_TYPE"),
            new ConfigMapping("notifier.ringBufferSize", "NOTIFIER_RING_BUFFER_SIZE"),
            new ConfigMapping("producer.type", "PRODUCER_TYPE"),
            new ConfigMapping("server.maxMessageSizeBytes", "SERVER_MAX_MESSAGE_SIZE_BYTES"),
            new ConfigMapping("server.port", "SERVER_PORT"),
            new ConfigMapping("prometheus.endpointEnabled", "PROMETHEUS_ENDPOINT_ENABLED"),
            new ConfigMapping("prometheus.endpointPortNumber", "PROMETHEUS_ENDPOINT_PORT_NUMBER"));

    private ServerMappedConfigSourceInitializer() {}

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
