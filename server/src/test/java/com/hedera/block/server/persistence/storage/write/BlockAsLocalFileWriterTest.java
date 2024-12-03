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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
        final int expectedBlockItems = 10;
        final List<BlockItemUnparsed> toWrite = generateBlockItemsUnparsed(1);

        assertThat(toWrite).isNotNull().isNotEmpty().satisfies(blockItemUnparsed -> {
            assertThat(blockItemUnparsed).isNotNull().isNotEmpty().hasSize(10);
            assertThat(blockItemUnparsed.getFirst().blockHeader()).isNotNull();
            assertThat(blockItemUnparsed.getLast().blockProof()).isNotNull();
        });

        final BlockHeader targetHeader =
                BlockHeader.PROTOBUF.parse(toWrite.getFirst().blockHeader());

        final Path expectedPath = pathResolverMock.resolvePathToBlock(targetHeader.number());
        assertThat(expectedPath).isNotNull().doesNotExist();

        final BlockUnparsed expectedBlockToWrite =
                BlockUnparsed.newBuilder().blockItems(toWrite).build();
        final Optional<List<BlockItemUnparsed>> actual = toTest.write(toWrite);
        assertThat(actual)
                .isNotNull()
                .isNotEmpty()
                .get(InstanceOfAssertFactories.list(BlockItemUnparsed.class))
                .isNotNull()
                .isNotEmpty()
                .hasSize(expectedBlockItems)
                .containsExactlyElementsOf(toWrite);
        assertThat(expectedPath)
                .isNotNull()
                .exists()
                .isNotEmptyFile()
                .isReadable()
                .hasBinaryContent(
                        BlockUnparsed.PROTOBUF.toBytes(expectedBlockToWrite).toByteArray());
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalFileWriter#write(List)} writes correctly a given Block
     * to the file system. The Block is written correctly if the input items to
     * write begin with a {@link BlockHeader}, end with a {@link BlockProof},
     * the Block file is written to the correct path and the input items are
     * returned. Items to can start with a block header and can end in a block
     * proof. If they start with a block header, but not end with block proof,
     * we expect nothing to be written and an empty optional to be returned.
     * If they do not start with a block header and do not end with a block
     * proof, we expect nothing to be written and an empty optional to be
     * returned. If they do not start with a block header, but end with a block
     * proof, we expect the block to be written and the input items to be
     * returned. After writing, all block items must be returned. We kind of
     * buffer the block items until we have a block proof in memory and then we
     * persist them to the fs.
     */
    @Test
    void testSuccessfulBlockWritePartial() throws IOException, ParseException {
        final int expectedBlockItems = 10;
        final List<BlockItemUnparsed> toWrite = generateBlockItemsUnparsed(1);

        assertThat(toWrite).isNotNull().isNotEmpty().satisfies(blockItemUnparsed -> {
            assertThat(blockItemUnparsed).isNotNull().isNotEmpty().hasSize(expectedBlockItems);
            assertThat(blockItemUnparsed.getFirst().blockHeader()).isNotNull();
            assertThat(blockItemUnparsed.getLast().blockProof()).isNotNull();
        });

        final List<BlockItemUnparsed> firstHalfToWrite = toWrite.subList(0, 5);
        final List<BlockItemUnparsed> secondHalfToWrite = toWrite.subList(5, toWrite.size());

        final BlockHeader targetHeader =
                BlockHeader.PROTOBUF.parse(toWrite.getFirst().blockHeader());

        final Path expectedPath = pathResolverMock.resolvePathToBlock(targetHeader.number());
        assertThat(expectedPath).isNotNull().doesNotExist();

        final BlockUnparsed expectedBlockToWrite =
                BlockUnparsed.newBuilder().blockItems(toWrite).build();

        final Optional<List<BlockItemUnparsed>> actualOnFirstWrite = toTest.write(firstHalfToWrite);
        assertThat(actualOnFirstWrite).isNotNull().isEmpty();
        assertThat(expectedPath).isNotNull().doesNotExist();

        final Optional<List<BlockItemUnparsed>> actualOnSecondWrite = toTest.write(secondHalfToWrite);
        assertThat(actualOnSecondWrite)
                .isNotNull()
                .isNotEmpty()
                .get(InstanceOfAssertFactories.list(BlockItemUnparsed.class))
                .isNotNull()
                .isNotEmpty()
                .hasSize(expectedBlockItems)
                .containsExactlyElementsOf(toWrite);
        assertThat(expectedPath)
                .isNotNull()
                .exists()
                .isNotEmptyFile()
                .isReadable()
                .hasBinaryContent(
                        BlockUnparsed.PROTOBUF.toBytes(expectedBlockToWrite).toByteArray());
    }

    /**
     * This test aims to verify that the
     * {@link BlockAsLocalDirWriter#write(List)} correctly throws an
     * {@link IllegalArgumentException} when an invalid block number is
     * provided. A block number is invalid if it is a strictly negative number.
     *
     * @param blockNumber parameterized, block number
     */
    @ParameterizedTest
    @MethodSource("invalidBlockNumbers")
    void testInvalidBlockNumber(final long blockNumber) throws IOException {
        final BlockHeader blockHeader =
                BlockHeader.newBuilder().number(blockNumber).build();
        final BlockItemUnparsed blockHeaderUnparsed = BlockItemUnparsed.newBuilder()
                .blockHeader(BlockHeader.PROTOBUF.toBytes(blockHeader))
                .build();

        final BlockProof blockProof = BlockProof.newBuilder().build();
        final BlockItemUnparsed blockProofUnparsed = BlockItemUnparsed.newBuilder()
                .blockProof(BlockProof.PROTOBUF.toBytes(blockProof))
                .build();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> toTest.write(List.of(blockHeaderUnparsed, blockProofUnparsed)));
    }

    /**
     * Some invalid block numbers.
     *
     * @return a stream of invalid block numbers
     */
    public static Stream<Arguments> invalidBlockNumbers() {
        return Stream.of(
                Arguments.of(-1L),
                Arguments.of(-2L),
                Arguments.of(-10L),
                Arguments.of(-100L),
                Arguments.of(-1_000L),
                Arguments.of(-10_000L),
                Arguments.of(-100_000L),
                Arguments.of(-1_000_000L),
                Arguments.of(-10_000_000L),
                Arguments.of(-100_000_000L),
                Arguments.of(-1_000_000_000L),
                Arguments.of(-10_000_000_000L),
                Arguments.of(-100_000_000_000L),
                Arguments.of(-1_000_000_000_000L),
                Arguments.of(-10_000_000_000_000L),
                Arguments.of(-100_000_000_000_000L),
                Arguments.of(-1_000_000_000_000_000L),
                Arguments.of(-10_000_000_000_000_000L),
                Arguments.of(-100_000_000_000_000_000L),
                Arguments.of(-1_000_000_000_000_000_000L),
                Arguments.of(Long.MIN_VALUE));
    }
}
