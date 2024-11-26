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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.swirlds.config.extensions.sources.ConfigMapping;
import com.swirlds.config.extensions.sources.MappedConfigSource;
import java.lang.reflect.Field;
import java.util.Queue;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ServerMappedConfigSourceInitializerTest {
    private static final ConfigMapping[] SUPPORTED_MAPPINGS = {
        new ConfigMapping("consumer.timeoutThresholdMillis", "CONSUMER_TIMEOUT_THRESHOLD_MILLIS"),
        new ConfigMapping("persistence.storage.rootPath", "PERSISTENCE_STORAGE_ROOT_PATH"),
        new ConfigMapping("service.delayMillis", "SERVICE_DELAY_MILLIS"),
        new ConfigMapping("mediator.ringBufferSize", "MEDIATOR_RING_BUFFER_SIZE"),
        new ConfigMapping("notifier.ringBufferSize", "NOTIFIER_RING_BUFFER_SIZE"),
        new ConfigMapping("server.maxMessageSizeBytes", "SERVER_MAX_MESSAGE_SIZE_BYTES"),
        new ConfigMapping("server.port", "SERVER_PORT"),
    };
    private static MappedConfigSource toTest;

    @BeforeAll
    static void setUp() {
        toTest = ServerMappedConfigSourceInitializer.getMappedConfigSource();
    }

    /**
     * This test aims to fail if we have added or removed any {@link ConfigMapping} that will be
     * initialized by the {@link ServerMappedConfigSourceInitializer#getMappedConfigSource()}
     * without reflecting it here in the test. The purpose is to bring attention to any changes to
     * the developer so we can make sure we are aware of them in order to be sure we require the
     * change. This test is more to bring attention than to test actual logic. So if this fails, we
     * either change the {@link #SUPPORTED_MAPPINGS} here or the {@link
     * ServerMappedConfigSourceInitializer#MAPPINGS} to make this pass.
     */
    @Test
    void test_VerifyAllSupportedMappingsAreAddedToInstance() throws ReflectiveOperationException {
        final Queue<ConfigMapping> actual = extractConfigMappings();

        assertEquals(SUPPORTED_MAPPINGS.length, actual.size());

        for (final ConfigMapping current : SUPPORTED_MAPPINGS) {
            final Predicate<ConfigMapping> predicate =
                    cm -> current.mappedName().equals(cm.mappedName())
                            && current.originalName().equals(cm.originalName());
            assertTrue(
                    actual.stream().anyMatch(predicate),
                    () -> "when testing for: [%s] it is not contained in mappings of the actual initialized object %s"
                            .formatted(current, actual));
        }
    }

    private static Queue<ConfigMapping> extractConfigMappings() throws ReflectiveOperationException {
        final Field configMappings = MappedConfigSource.class.getDeclaredField("configMappings");
        try {
            configMappings.setAccessible(true);
            return (Queue<ConfigMapping>) configMappings.get(toTest);
        } finally {
            configMappings.setAccessible(false);
        }
    }
}
