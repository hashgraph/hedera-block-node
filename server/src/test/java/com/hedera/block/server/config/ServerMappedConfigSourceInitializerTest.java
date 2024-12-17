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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.swirlds.common.metrics.config.MetricsConfig;
import com.swirlds.common.metrics.platform.prometheus.PrometheusConfig;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;
import com.swirlds.config.extensions.sources.ConfigMapping;
import com.swirlds.config.extensions.sources.MappedConfigSource;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link ServerMappedConfigSourceInitializer}.
 */
class ServerMappedConfigSourceInitializerTest {
    private static final ConfigMapping[] SUPPORTED_MAPPINGS = {
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
        new ConfigMapping("prometheus.endpointPortNumber", "PROMETHEUS_ENDPOINT_PORT_NUMBER")
    };

    /**
     * This test aims to verify the state of all config extensions we have
     * added. These configs are the ones that are returned from
     * {@link BlockNodeConfigExtension#getConfigDataTypes()}. This test will
     * verify:
     * <pre>
     *     - all added config classes are annotated with the {@link ConfigData}
     *       annotation.
     *     - all fields in all config classes are annotated with the
     *       {@link ConfigProperty} annotation.
     *     - a mapping for all fields in all config classes is present in the
     *       {@link ServerMappedConfigSourceInitializer#MAPPINGS()}.
     * </pre>
     * @param config parameterized, config class to test
     */
    @ParameterizedTest
    @MethodSource("allConfigDataTypes")
    void testVerifyAllFieldsInRecordsAreMapped(final Class<? extends Record> config) {
        if (!config.isAnnotationPresent(ConfigData.class)) {
            fail("Class %s is missing the ConfigData annotation! All config classes MUST have that annotation present!"
                    .formatted(config.getSimpleName()));
        } else {
            final ConfigData configDataAnnotation = config.getDeclaredAnnotation(ConfigData.class);
            final String prefix = configDataAnnotation.value();
            for (final RecordComponent recordComponent : config.getRecordComponents()) {
                if (!recordComponent.isAnnotationPresent(ConfigProperty.class)) {
                    fail(
                            "Field %s in %s is missing the ConfigProperty annotation! All fields in config classes MUST have that annotation present!"
                                    .formatted(recordComponent.getName(), config.getSimpleName()));
                } else {
                    final String expectedMappedName = "%s.%s".formatted(prefix, recordComponent.getName());
                    final Optional<ConfigMapping> matchingMapping = Arrays.stream(SUPPORTED_MAPPINGS)
                            .filter(mapping -> mapping.mappedName().equals(expectedMappedName))
                            .findFirst();
                    assertThat(matchingMapping)
                            .isNotNull()
                            .withFailMessage(
                                    "Field [%s] in [%s] is not present in the environment variable mappings! Expected config key [%s] to be present and to be mapped to [%s]",
                                    recordComponent.getName(),
                                    config.getSimpleName(),
                                    expectedMappedName,
                                    transformToEnvVarConvention(expectedMappedName))
                            .isPresent();
                }
            }
        }
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
    void testVerifyAllSupportedMappingsAreAddedToInstance() throws ReflectiveOperationException {
        final Queue<ConfigMapping> actual = extractConfigMappings();

        // fail if the actual and this test have a different number of mappings
        assertThat(SUPPORTED_MAPPINGS.length)
                .withFailMessage(
                        "The number of supported mappings has changed! Please update the test to reflect the change.\nRUNTIME_MAPPING: %s\nTEST_MAPPING: %s",
                        actual, Arrays.toString(SUPPORTED_MAPPINGS))
                .isEqualTo(actual.size());

        // test this test against actual
        for (final ConfigMapping current : SUPPORTED_MAPPINGS) {
            final Predicate<ConfigMapping> predicate =
                    cm -> current.mappedName().equals(cm.mappedName())
                            && current.originalName().equals(cm.originalName());
            assertThat(actual.stream().anyMatch(predicate))
                    .withFailMessage(
                            "When testing for: [%s] it is not contained in mappings of the actual initialized object %s",
                            current, actual)
                    .isTrue();
        }

        // test actual against this test
        for (final ConfigMapping current : actual) {
            final Predicate<ConfigMapping> predicate =
                    cm -> current.mappedName().equals(cm.mappedName())
                            && current.originalName().equals(cm.originalName());
            assertThat(Arrays.stream(SUPPORTED_MAPPINGS).anyMatch(predicate))
                    .withFailMessage(
                            "When testing for: [%s] it is not contained in mappings of this test %s", current, actual)
                    .isTrue();
        }
    }

    private static String transformToEnvVarConvention(final String input) {
        String underscored = input.replace(".", "_");
        String resolved = underscored.replaceAll("(?<!_)([A-Z])", "_$1");
        return resolved.toUpperCase();
    }

    @SuppressWarnings("unchecked")
    private static Queue<ConfigMapping> extractConfigMappings() throws ReflectiveOperationException {
        final Field configMappings = MappedConfigSource.class.getDeclaredField("configMappings");
        try {
            configMappings.setAccessible(true);
            return (Queue<ConfigMapping>)
                    configMappings.get(ServerMappedConfigSourceInitializer.getMappedConfigSource());
        } finally {
            configMappings.setAccessible(false);
        }
    }

    private static Stream<Arguments> allConfigDataTypes() {
        // Add any classes that should be excluded from the test for any reason in the set below
        // MetricsConfig and PrometheusConfig are not managed by us
        final Set<Class<? extends Record>> excluded = Set.of(MetricsConfig.class, PrometheusConfig.class);
        return new BlockNodeConfigExtension()
                .getConfigDataTypes().stream()
                        .filter(configType -> !excluded.contains(configType))
                        .map(Arguments::of);
    }
}
