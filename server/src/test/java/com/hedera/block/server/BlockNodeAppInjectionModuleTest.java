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

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.swirlds.config.api.Configuration;
import io.helidon.webserver.WebServerConfig;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class BlockNodeAppInjectionModuleTest {

    @Mock private StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> streamMediator;

    @Mock private BlockReader<Block> blockReader;

    @Mock private ServiceStatus serviceStatus;

    private BlockNodeContext blockNodeContext;

    @BeforeEach
    void setUp() throws IOException {
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
    }

    @Test
    void testProvideBlockNodeContext() {
        Configuration configuration = blockNodeContext.configuration();
        MetricsService metricsService = blockNodeContext.metricsService();

        BlockNodeContext providedBlockNodeContext =
                BlockNodeAppInjectionModule.provideBlockNodeContext(configuration, metricsService);

        Assertions.assertEquals(blockNodeContext, providedBlockNodeContext);
        Assertions.assertEquals(
                blockNodeContext.configuration(), providedBlockNodeContext.configuration());
        Assertions.assertEquals(
                blockNodeContext.metricsService(), providedBlockNodeContext.metricsService());
    }

    @Test
    void testProvideBlockStreamService() {
        BlockStreamService blockStreamService =
                BlockNodeAppInjectionModule.provideBlockStreamService(
                        streamMediator, blockReader, serviceStatus, blockNodeContext);

        Assertions.assertNotNull(blockStreamService);
    }

    @Test
    void testProvideWebServerConfigBuilder() {
        WebServerConfig.Builder webServerConfigBuilder =
                BlockNodeAppInjectionModule.provideWebServerConfigBuilder();

        Assertions.assertNotNull(webServerConfigBuilder);
    }
}
