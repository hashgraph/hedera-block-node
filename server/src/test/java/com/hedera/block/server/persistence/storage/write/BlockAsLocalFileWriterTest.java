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

package com.hedera.block.server.persistence.storage.write;

import static com.hedera.block.server.util.PersistTestUtils.PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY;
import static com.hedera.block.server.util.PersistTestUtils.generateBlockItemsUnparsed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.path.BlockAsLocalFilePathResolver;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test class for {@link BlockAsLocalFileWriter}.
 */
class BlockAsLocalFileWriterTest {
    private BlockNodeContext blockNodeContext;
    private PersistenceStorageConfig testConfig;
    private BlockAsLocalFilePathResolver pathResolverMock;
    private BlockRemover blockRemoverMock;

    @TempDir
    private Path testLiveRootPath;

    private BlockAsLocalFileWriter toTest;

    @BeforeEach
    void setUp() throws IOException {
        blockNodeContext = TestConfigUtil.getTestBlockNodeContext(
                Map.of(PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY, testLiveRootPath.toString()));
        testConfig = blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);

        blockRemoverMock = mock(BlockRemover.class);

        final String testConfigLiveRootPath = testConfig.liveRootPath();
        assertThat(testConfigLiveRootPath).isEqualTo(testLiveRootPath.toString());
        pathResolverMock = spy(BlockAsLocalFilePathResolver.of(testLiveRootPath));

        toTest = BlockAsLocalFileWriter.of(blockNodeContext, blockRemoverMock, pathResolverMock);
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFileWriter#write(List)} writes correctly a given Block
     * to the file system. The Block is written correctly if the input items to
     * write begin with a {@link BlockHeader}, end with a {@link BlockProof},
     * the Block file is written to the correct path and the input items are
     * returned.
     */
    @Test
    void testSuccessfulBlockWrite() throws IOException, ParseException {
        final List<BlockItemUnparsed> toWrite = generateBlockItemsUnparsed(1);

        assertThat(toWrite).isNotNull().isNotEmpty().satisfies(blockItemUnparsed -> {
            assertThat(blockItemUnparsed.getFirst().blockHeader()).isNotNull();
            assertThat(blockItemUnparsed.getLast().blockProof()).isNotNull();
        });

        final BlockHeader targetHeader =
                BlockHeader.PROTOBUF.parse(toWrite.getFirst().blockHeader());

        final Path expectedPath = pathResolverMock.resolvePathToBlock(targetHeader.number());
        final BlockUnparsed expectedBlockToWrite =
                BlockUnparsed.newBuilder().blockItems(toWrite).build();
        final Optional<List<BlockItemUnparsed>> actual = toTest.write(toWrite);
        assertThat(actual).isNotNull().isNotEmpty().containsSame(toWrite);
        assertThat(expectedPath)
                .isNotNull()
                .exists()
                .isNotEmptyFile()
                .isReadable()
                .hasBinaryContent(
                        BlockUnparsed.PROTOBUF.toBytes(expectedBlockToWrite).toByteArray());
    }
}
