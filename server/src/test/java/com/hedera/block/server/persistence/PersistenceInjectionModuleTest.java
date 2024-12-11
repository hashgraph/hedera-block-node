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

package com.hedera.block.server.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.CompressionType;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.StorageType;
import com.hedera.block.server.persistence.storage.compression.Compression;
import com.hedera.block.server.persistence.storage.compression.NoOpCompression;
import com.hedera.block.server.persistence.storage.compression.ZstdCompression;
import com.hedera.block.server.persistence.storage.path.BlockAsLocalDirPathResolver;
import com.hedera.block.server.persistence.storage.path.BlockAsLocalFilePathResolver;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.block.server.persistence.storage.path.NoOpBlockPathResolver;
import com.hedera.block.server.persistence.storage.read.BlockAsLocalDirReader;
import com.hedera.block.server.persistence.storage.read.BlockAsLocalFileReader;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.read.NoOpBlockReader;
import com.hedera.block.server.persistence.storage.remove.BlockAsLocalDirRemover;
import com.hedera.block.server.persistence.storage.remove.BlockAsLocalFileRemover;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.persistence.storage.remove.NoOpBlockRemover;
import com.hedera.block.server.persistence.storage.write.BlockAsLocalDirWriter;
import com.hedera.block.server.persistence.storage.write.BlockAsLocalFileWriter;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.persistence.storage.write.NoOpBlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.swirlds.config.api.Configuration;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistenceInjectionModuleTest {
    @Mock
    private BlockNodeContext blockNodeContextMock;

    @Mock
    private PersistenceStorageConfig persistenceStorageConfigMock;

    @Mock
    private BlockRemover blockRemoverMock;

    @Mock
    private BlockPathResolver blockPathResolverMock;

    @Mock
    private Compression compressionMock;

    @Mock
    private SubscriptionHandler<SubscribeStreamResponseUnparsed> subscriptionHandlerMock;

    @Mock
    private Notifier notifierMock;

    @Mock
    private BlockWriter<List<BlockItemUnparsed>> blockWriterMock;

    @Mock
    private ServiceStatus serviceStatusMock;

    @TempDir
    private Path testLiveRootPath;

    /**
     * This test aims to verify that the
     * {@link PersistenceInjectionModule#providesBlockWriter} method will return
     * the correct {@link BlockWriter} instance based on the {@link StorageType}
     * parameter. The test verifies only the result type and not what is inside
     * the instance! For the purpose of this test, what is inside the instance
     * is not important. We aim to test the branch that will be taken based on
     * the {@link StorageType} parameter in terms of the returned instance type.
     *
     * @param storageType parameterized, the {@link StorageType} to test
     */
    @ParameterizedTest
    @MethodSource("storageTypes")
    void testProvidesBlockWriter(final StorageType storageType) {
        final Configuration localConfigurationMock = mock(Configuration.class);
        lenient().when(blockNodeContextMock.metricsService()).thenReturn(mock(MetricsService.class));
        when(blockNodeContextMock.configuration()).thenReturn(localConfigurationMock);
        when(localConfigurationMock.getConfigData(PersistenceStorageConfig.class))
                .thenReturn(persistenceStorageConfigMock);

        lenient().when(persistenceStorageConfigMock.liveRootPath()).thenReturn(testLiveRootPath.toString());
        when(persistenceStorageConfigMock.type()).thenReturn(storageType);

        final BlockWriter<List<BlockItemUnparsed>> actual = PersistenceInjectionModule.providesBlockWriter(
                blockNodeContextMock, blockRemoverMock, blockPathResolverMock, compressionMock);

        final Class<?> targetInstanceType =
                switch (storageType) {
                    case BLOCK_AS_LOCAL_DIRECTORY -> BlockAsLocalDirWriter.class;
                    case BLOCK_AS_LOCAL_FILE -> BlockAsLocalFileWriter.class;
                    case NO_OP -> NoOpBlockWriter.class;
                };
        assertThat(actual).isNotNull().isExactlyInstanceOf(targetInstanceType);
    }

    /**
     * This test aims to verify that the
     * {@link PersistenceInjectionModule#providesBlockWriter} throws an
     * {@link java.io.UncheckedIOException} if there is a problem with the
     * creation of the {@link BlockWriter} instance.
     */
    @Test
    void testProvidesBlockWriter_IOException() {
        final BlockNodeContext blockNodeContextMock = mock(BlockNodeContext.class);

        final PersistenceStorageConfig localPersistenceStorageConfigMock = mock(PersistenceStorageConfig.class);
        when(localPersistenceStorageConfigMock.liveRootPath()).thenReturn("/invalid_path/:invalid_directory");
        when(localPersistenceStorageConfigMock.type()).thenReturn(StorageType.BLOCK_AS_LOCAL_DIRECTORY);

        final Configuration configuration = mock(Configuration.class);
        when(blockNodeContextMock.configuration()).thenReturn(configuration);
        when(configuration.getConfigData(PersistenceStorageConfig.class)).thenReturn(localPersistenceStorageConfigMock);

        final MetricsService metricsServiceMock = mock(MetricsService.class);
        when(blockNodeContextMock.metricsService()).thenReturn(metricsServiceMock);

        // Expect an UncheckedIOException due to the IOException
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> PersistenceInjectionModule.providesBlockWriter(
                        blockNodeContextMock, blockRemoverMock, blockPathResolverMock, compressionMock))
                .withCauseInstanceOf(IOException.class)
                .withMessage("Failed to create BlockWriter");
    }

    /**
     * This test aims to verify that the
     * {@link PersistenceInjectionModule#providesBlockReader} method will return
     * the correct {@link BlockReader} instance based on the {@link StorageType}
     * parameter. The test verifies only the result type and not what is inside
     * the instance! For the purpose of this test, what is inside the instance
     * is not important. We aim to test the branch that will be taken based on
     * the {@link StorageType} parameter in terms of the returned instance type.
     *
     * @param storageType parameterized, the {@link StorageType} to test
     */
    @ParameterizedTest
    @MethodSource("storageTypes")
    void testProvidesBlockReader(final StorageType storageType) {
        lenient().when(persistenceStorageConfigMock.liveRootPath()).thenReturn(testLiveRootPath.toString());
        when(persistenceStorageConfigMock.type()).thenReturn(storageType);

        final BlockReader<BlockUnparsed> actual =
                PersistenceInjectionModule.providesBlockReader(persistenceStorageConfigMock, blockPathResolverMock);

        final Class<?> targetInstanceType =
                switch (storageType) {
                    case BLOCK_AS_LOCAL_DIRECTORY -> BlockAsLocalDirReader.class;
                    case BLOCK_AS_LOCAL_FILE -> BlockAsLocalFileReader.class;
                    case NO_OP -> NoOpBlockReader.class;
                };
        assertThat(actual).isNotNull().isExactlyInstanceOf(targetInstanceType);
    }

    /**
     * This test aims to verify that the
     * {@link PersistenceInjectionModule#providesBlockRemover} method will
     * return the correct {@link BlockRemover} instance based on the
     * {@link StorageType} parameter. The test verifies only the result type and
     * not what is inside the instance! For the purpose of this test, what is
     * inside the instance is not important. We aim to test the branch that will
     * be taken based on the {@link StorageType} parameter in terms of the
     * returned instance type.
     *
     * @param storageType parameterized, the {@link StorageType} to test
     */
    @ParameterizedTest
    @MethodSource("storageTypes")
    void testProvidesBlockRemover(final StorageType storageType) {
        when(persistenceStorageConfigMock.type()).thenReturn(storageType);

        final BlockRemover actual =
                PersistenceInjectionModule.providesBlockRemover(persistenceStorageConfigMock, blockPathResolverMock);

        final Class<?> targetInstanceType =
                switch (storageType) {
                    case BLOCK_AS_LOCAL_DIRECTORY -> BlockAsLocalDirRemover.class;
                    case BLOCK_AS_LOCAL_FILE -> BlockAsLocalFileRemover.class;
                    case NO_OP -> NoOpBlockRemover.class;
                };
        assertThat(actual).isNotNull().isExactlyInstanceOf(targetInstanceType);
    }

    /**
     * This test aims to verify that the
     * {@link PersistenceInjectionModule#providesPathResolver(PersistenceStorageConfig)}
     * method will return the correct {@link BlockPathResolver} instance based
     * on the {@link StorageType} parameter. The test verifies only the result
     * type and not what is inside the instance! For the purpose of this test,
     * what is inside the instance is not important. We aim to test the branch
     * that will be taken based on the {@link StorageType} parameter in terms of
     * the returned instance type.
     *
     * @param storageType parameterized, the {@link StorageType} to test
     */
    @ParameterizedTest
    @MethodSource("storageTypes")
    void testProvidesBlockPathResolver(final StorageType storageType) {
        lenient().when(persistenceStorageConfigMock.liveRootPath()).thenReturn(testLiveRootPath.toString());
        when(persistenceStorageConfigMock.type()).thenReturn(storageType);

        final BlockPathResolver actual = PersistenceInjectionModule.providesPathResolver(persistenceStorageConfigMock);

        final Class<?> targetInstanceType =
                switch (storageType) {
                    case BLOCK_AS_LOCAL_DIRECTORY -> BlockAsLocalDirPathResolver.class;
                    case BLOCK_AS_LOCAL_FILE -> BlockAsLocalFilePathResolver.class;
                    case NO_OP -> NoOpBlockPathResolver.class;
                };
        assertThat(actual).isNotNull().isExactlyInstanceOf(targetInstanceType);
    }

    /**
     * This test aims to verify that the
     * {@link PersistenceInjectionModule#providesCompression(PersistenceStorageConfig)}
     * method will return the correct {@link Compression} instance based on the
     * {@link CompressionType} parameter. The test verifies only the result type
     * and not what is inside the instance! For the purpose of this test, what
     * is inside the instance is not important. We aim to test the branch that
     * will be taken based on the {@link CompressionType} parameter in terms of
     * the returned instance type.
     *
     * @param compressionType parameterized, the {@link CompressionType} to test
     */
    @ParameterizedTest
    @MethodSource("compressionTypes")
    void testProvidesCompression(final CompressionType compressionType) {
        when(persistenceStorageConfigMock.compression()).thenReturn(compressionType);
        final Compression actual = PersistenceInjectionModule.providesCompression(persistenceStorageConfigMock);

        final Class<?> targetInstanceType =
                switch (compressionType) {
                    case ZSTD -> ZstdCompression.class;
                    case NONE -> NoOpCompression.class;
                };
        assertThat(actual).isNotNull().isExactlyInstanceOf(targetInstanceType);
    }

    @Test
    void testProvidesStreamValidatorBuilder() throws IOException {
        final BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();

        // Call the method under test
        final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> streamVerifier =
                new StreamPersistenceHandlerImpl(
                        subscriptionHandlerMock, notifierMock, blockWriterMock, blockNodeContext, serviceStatusMock);
        assertNotNull(streamVerifier);
    }

    /**
     * All {@link StorageType} dynamically generated.
     */
    private static Stream<Arguments> storageTypes() {
        return Arrays.stream(StorageType.values()).map(Arguments::of);
    }

    /**
     * All {@link CompressionType} dynamically generated.
     */
    private static Stream<Arguments> compressionTypes() {
        return Arrays.stream(CompressionType.values()).map(Arguments::of);
    }
}
