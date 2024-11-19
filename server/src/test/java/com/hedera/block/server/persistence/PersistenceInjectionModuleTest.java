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

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.BlocksPersisted;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.StorageType;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.remove.NoOpRemover;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.swirlds.config.api.Configuration;
import com.swirlds.metrics.api.Counter;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistenceInjectionModuleTest {

    @Mock
    private BlockNodeContext blockNodeContext;

    @Mock
    private PersistenceStorageConfig persistenceStorageConfig;

    @Mock
    private SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler;

    @Mock
    private Notifier notifier;

    @Mock
    private BlockWriter<List<BlockItem>> blockWriter;

    @Mock
    private ServiceStatus serviceStatus;

    @BeforeEach
    void setup() throws IOException {
        // Setup any necessary mocks before each test
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        persistenceStorageConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
    }

    @Test
    void testProvidesBlockWriter() {

        BlockWriter<List<BlockItem>> blockWriter =
                PersistenceInjectionModule.providesBlockWriter(blockNodeContext, new NoOpRemover());

        assertNotNull(blockWriter);
    }

    @Test
    void testProvidesBlockWriter_IOException() {
        final BlockNodeContext blockNodeContext = mock(BlockNodeContext.class);

        final PersistenceStorageConfig persistenceStorageConfig = mock(PersistenceStorageConfig.class);
        when(persistenceStorageConfig.rootPath()).thenReturn("/invalid_path/:invalid_directory");
        when(persistenceStorageConfig.type()).thenReturn(StorageType.BLOCK_AS_DIR);

        final Configuration configuration = mock(Configuration.class);
        when(blockNodeContext.configuration()).thenReturn(configuration);
        when(configuration.getConfigData(PersistenceStorageConfig.class)).thenReturn(persistenceStorageConfig);

        final MetricsService metricsServiceMock = mock(MetricsService.class);
        when(blockNodeContext.metricsService()).thenReturn(metricsServiceMock);
        when(metricsServiceMock.get(BlocksPersisted)).thenReturn(mock(Counter.class));

        // Expect a RuntimeException due to the IOException
        assertThatRuntimeException()
                .isThrownBy(() -> {
                    PersistenceInjectionModule.providesBlockWriter(blockNodeContext, new NoOpRemover());
                })
                .withCauseInstanceOf(IOException.class)
                .withMessage("Failed to create BlockWriter");
    }

    @Test
    void testProvidesBlockReader() {

        BlockReader<Block> blockReader = PersistenceInjectionModule.providesBlockReader(persistenceStorageConfig);

        assertNotNull(blockReader);
    }

    @Test
    void testProvidesStreamValidatorBuilder() throws IOException {

        BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();

        // Call the method under test
        BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> streamVerifier = new StreamPersistenceHandlerImpl(
                subscriptionHandler, notifier, blockWriter, blockNodeContext, serviceStatus);

        assertNotNull(streamVerifier);
    }
}
