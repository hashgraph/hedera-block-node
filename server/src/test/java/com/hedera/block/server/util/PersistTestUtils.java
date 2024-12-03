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

package com.hedera.block.server.util;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.INFO;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.hedera.block.server.persistence.storage.path.BlockAsLocalDirPathResolver;
import com.hedera.block.server.persistence.storage.path.BlockAsLocalFilePathResolver;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.block.stream.input.EventHeader;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.hapi.node.base.SemanticVersion;
import com.hedera.hapi.platform.event.EventCore;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PersistTestUtils {
    private static final Logger LOGGER = System.getLogger(PersistTestUtils.class.getName());
    public static final String PERSISTENCE_STORAGE_LIVE_ROOT_PATH_KEY = "persistence.storage.liveRootPath";

    private PersistTestUtils() {}

    public static void writeBlockItemToPath(final Path path, final BlockItemUnparsed blockItem) throws IOException {

        Bytes bytes = BlockItemUnparsed.PROTOBUF.toBytes(blockItem);
        writeBytesToPath(path, bytes.toByteArray());
    }

    public static void writeBytesToPath(final Path path, final byte[] bytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path.toString())) {
            fos.write(bytes);
            LOGGER.log(INFO, "Successfully wrote the bytes to file: {0}", path);
        }
    }

    public static List<BlockItemUnparsed> generateBlockItemsUnparsed(int numOfBlocks) {

        List<BlockItemUnparsed> blockItems = new ArrayList<>();
        for (int i = 1; i <= numOfBlocks; i++) {
            for (int j = 1; j <= 10; j++) {
                switch (j) {
                    case 1:
                        // First block is always the header
                        blockItems.add(BlockItemUnparsed.newBuilder()
                                .blockHeader(BlockHeader.PROTOBUF.toBytes(BlockHeader.newBuilder()
                                        .number(i)
                                        .softwareVersion(SemanticVersion.newBuilder()
                                                .major(1)
                                                .minor(0)
                                                .build())
                                        .build()))
                                .build());
                        break;
                    case 10:
                        // Last block is always the state proof
                        blockItems.add(BlockItemUnparsed.newBuilder()
                                .blockProof(BlockProof.PROTOBUF.toBytes(
                                        BlockProof.newBuilder().block(i).build()))
                                .build());
                        break;
                    default:
                        // Middle blocks are events
                        blockItems.add(BlockItemUnparsed.newBuilder()
                                .eventHeader(EventHeader.PROTOBUF.toBytes(EventHeader.newBuilder()
                                        .eventCore(EventCore.newBuilder()
                                                .creatorNodeId(i)
                                                .build())
                                        .build()))
                                .build());
                        break;
                }
            }
        }

        return blockItems;
    }

    public static byte[] reverseByteArray(byte[] input) {
        if (input == null || input.length == 0) {
            return input;
        }

        byte[] reversed = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            reversed[i] = input[input.length - 1 - i];
        }

        return reversed;
    }

    /**
     * This method spies and trains a {@link BlockAsLocalDirPathResolver}. It
     * requires a path to the live root test directory to use as base (usually
     * it would be a temp dir). The spy calls the real method when
     * {@link BlockAsLocalDirPathResolver#resolvePathToBlock(long)} is called.
     * The mock captures anyLong input so it is dynamic in that sense.
     *
     * @param liveRootTestPath path to the live root test directory
     * @return a trained mock that will return the resolved path to a block by a
     * given block number
     */
    public static BlockAsLocalDirPathResolver getTrainedBlockAsLocalDirPathResolver(
            @NonNull final Path liveRootTestPath) {
        Objects.requireNonNull(liveRootTestPath);
        final BlockAsLocalDirPathResolver result = spy(BlockAsLocalDirPathResolver.of(liveRootTestPath));
        when(result.resolvePathToBlock(anyLong())).thenCallRealMethod();
        return result;
    }

    /**
     * This method spies and trains a {@link BlockAsLocalFilePathResolver}. It
     * requires a path to the live root test directory to use as base (usually
     * it would be a temp dir). The spy calls the real method when
     * {@link BlockAsLocalFilePathResolver#resolvePathToBlock(long)} is called.
     * The mock captures anyLong input so it is dynamic in that sense.
     *
     * @param liveRootTestPath path to the live root test directory
     * @return a trained mock that will return the resolved path to a block by a
     * given block number
     */
    public static BlockAsLocalFilePathResolver getTrainedBlockAsLocalFilePathResolver(final Path liveRootTestPath) {
        Objects.requireNonNull(liveRootTestPath);
        final BlockAsLocalFilePathResolver result = spy(BlockAsLocalFilePathResolver.of(liveRootTestPath));
        when(result.resolvePathToBlock(anyLong())).thenCallRealMethod();
        return result;
    }
}
