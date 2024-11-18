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

package com.hedera.block.server.grpc;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BlockStreamServiceTest {

    //    @Mock
    //    private Notifier notifier;
    //
    //    @Mock
    //    private Flow.Subscriber<SingleBlockResponse> responseObserver;
    //
    //    @Mock
    //    private LiveStreamMediator streamMediator;
    //
    //    @Mock
    //    private BlockReader<Block> blockReader;
    //
    //    @Mock
    //    private BlockWriter<List<BlockItem>> blockWriter;
    //
    //    @Mock
    //    private ServiceStatus serviceStatus;
    //
    //    private final Logger LOGGER = System.getLogger(getClass().getName());
    //
    //    private static final int testTimeout = 1000;
    //
    //    private PbjBlockStreamService blockStreamService;
    //
    //    @TempDir
    //    private Path testPath;
    //
    //    private BlockNodeContext blockNodeContext;
    //    private PersistenceStorageConfig config;
    //
    //    @BeforeEach
    //    public void setUp() throws IOException {
    //        blockNodeContext =
    //                TestConfigUtil.getTestBlockNodeContext(Map.of("persistence.storage.rootPath",
    // testPath.toString()));
    //        config = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
    //
    //        final var blockNodeEventHandler = new StreamPersistenceHandlerImpl(
    //                streamMediator, notifier, blockWriter, blockNodeContext, serviceStatus);
    //        blockStreamService = new PbjBlockStreamServiceProxy(
    //                streamMediator, serviceStatus, blockNodeEventHandler, notifier, blockNodeContext);
    //    }
    //
    //    @Test
    //    public void testServiceName() {
    //        assertEquals(SERVICE_NAME_BLOCK_STREAM, blockStreamService.serviceName());
    //    }
    //
    //    @Test
    //    public void testFullName() {
    //        assertEquals(FULL_SERVICE_NAME_BLOCK_STREAM, blockStreamService.fullName());
    //    }
    //
    //    @Test
    //    public void testMethods() {
    //        assertEquals(2, blockStreamService.methods().size());
    //    }
}
