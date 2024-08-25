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

import static org.junit.jupiter.api.Assertions.*;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistenceInjectionModuleTest {

    @Mock private BlockNodeContext blockNodeContext;

    @Mock private PersistenceStorageConfig persistenceStorageConfig;

    @BeforeEach
    void setup() throws IOException {
        // Setup any necessary mocks before each test
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext();
        persistenceStorageConfig =
                blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
    }

    @Test
    void testProvidesBlockWriter() {

        BlockWriter<BlockItem> blockWriter =
                PersistenceInjectionModule.providesBlockWriter(blockNodeContext);

        assertNotNull(blockWriter);
    }

    @Test
    void testProvidesBlockReader() {

        BlockReader<Block> blockReader =
                PersistenceInjectionModule.providesBlockReader(persistenceStorageConfig);

        assertNotNull(blockReader);
    }
}
