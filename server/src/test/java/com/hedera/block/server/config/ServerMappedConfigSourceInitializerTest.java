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
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Queue;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ServerMappedConfigSourceInitializerTest {
    private static final ConfigMapping[] SUPPORTED_MAPPINGS = {
        new ConfigMapping("mediator.ringBufferSize", "MEDIATOR_RING_BUFFER_SIZE"),
        new ConfigMapping("notifier.ringBufferSize", "NOTIFIER_RING_BUFFER_SIZE")
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

        Assertions.assertEquals(SUPPORTED_MAPPINGS.length, actual.size());

        for (final ConfigMapping current : SUPPORTED_MAPPINGS) {
            final Predicate<ConfigMapping> predicate =
                    cm ->
                            current.mappedName().equals(cm.mappedName())
                                    && current.originalName().equals(cm.originalName());
            Assertions.assertTrue(
                    actual.stream().anyMatch(predicate),
                    "when testing for: ["
                            + current
                            + "] it is not contained in mappings of the actual initialized object "
                            + actual);
        }
    }

    @Test
    void test_VerifyNoInstanceCanBeCreated() {
        final Constructor<?>[] declaredConstructors =
                ServerMappedConfigSourceInitializer.class.getDeclaredConstructors();
        Assertions.assertEquals(1, declaredConstructors.length);

        final Constructor<?> privateNoArgsConstructor = declaredConstructors[0];
        Assertions.assertEquals(0, privateNoArgsConstructor.getParameterCount());
        Assertions.assertTrue(privateNoArgsConstructor.accessFlags().contains(AccessFlag.PRIVATE));
        try {
            privateNoArgsConstructor.setAccessible(true);
            Assertions.assertThrows(
                    InvocationTargetException.class, privateNoArgsConstructor::newInstance);
        } finally {
            privateNoArgsConstructor.setAccessible(false);
        }
    }

    private static Queue<ConfigMapping> extractConfigMappings()
            throws ReflectiveOperationException {
        final Field configMappings = MappedConfigSource.class.getDeclaredField("configMappings");
        try {
            configMappings.setAccessible(true);
            return (Queue<ConfigMapping>) configMappings.get(toTest);
        } finally {
            configMappings.setAccessible(false);
        }
    }
}
