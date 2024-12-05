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

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.BlocksPersisted;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A Block writer that handles writing of block-as-file.
 */
public final class BlockAsLocalFileWriter implements LocalBlockWriter<List<BlockItemUnparsed>> {
    private final MetricsService metricsService;
    private final BlockPathResolver blockPathResolver;
    private List<BlockItemUnparsed> currentBlockItems;
    private long currentBlockNumber = -1;

    /**
     * Constructor.
     *
     * @param blockNodeContext valid, {@code non-null} instance of {@link BlockNodeContext} used to
     * get the {@link MetricsService}
     * @param blockPathResolver valid, {@code non-null} instance of {@link BlockPathResolver} used
     * to resolve paths to Blocks
     */
    private BlockAsLocalFileWriter(
            @NonNull final BlockNodeContext blockNodeContext, @NonNull final BlockPathResolver blockPathResolver) {
        this.metricsService = Objects.requireNonNull(blockNodeContext.metricsService());
        this.blockPathResolver = Objects.requireNonNull(blockPathResolver);
    }

    /**
     * This method creates and returns a new instance of {@link BlockAsLocalFileWriter}.
     *
     * @param blockNodeContext valid, {@code non-null} instance of
     * {@link BlockNodeContext} used to get the {@link MetricsService}
     * @param blockPathResolver valid, {@code non-null} instance of
     * {@link BlockPathResolver} used to resolve paths to Blocks
     * @return a new, fully initialized instance of {@link BlockAsLocalFileWriter}
     */
    public static BlockAsLocalFileWriter of(
            @NonNull final BlockNodeContext blockNodeContext, @NonNull final BlockPathResolver blockPathResolver) {
        return new BlockAsLocalFileWriter(blockNodeContext, blockPathResolver);
    }

    @NonNull
    @Override
    public Optional<List<BlockItemUnparsed>> write(@NonNull final List<BlockItemUnparsed> valueToWrite)
            throws IOException, ParseException {
        final BlockItemUnparsed firstItem = valueToWrite.getFirst();
        if (firstItem.hasBlockHeader()) {
            currentBlockNumber = Preconditions.requireWhole(
                    BlockHeader.PROTOBUF.parse(firstItem.blockHeader()).number());
            currentBlockItems = new LinkedList<>(valueToWrite);
        } else {
            currentBlockItems.addAll(valueToWrite);
        }

        if (valueToWrite.getLast().hasBlockProof()) {
            final Optional<List<BlockItemUnparsed>> result = Optional.of(writeToFs());
            metricsService.get(BlocksPersisted).increment();
            resetState();
            return result;
        } else {
            return Optional.empty();
        }
    }

    private List<BlockItemUnparsed> writeToFs() throws IOException {
        final Path blockToWritePathResolved = blockPathResolver.resolvePathToBlock(currentBlockNumber);
        Files.createDirectories(blockToWritePathResolved.getParent());
        Files.createFile(blockToWritePathResolved);
        try (final FileOutputStream fos = new FileOutputStream(blockToWritePathResolved.toFile())) {
            final BlockUnparsed blockToWrite =
                    BlockUnparsed.newBuilder().blockItems(currentBlockItems).build();
            BlockUnparsed.PROTOBUF.toBytes(blockToWrite).writeTo(fos);
        }
        return currentBlockItems;
    }

    private void resetState() {
        currentBlockItems = null;
        currentBlockNumber = -1;
    }
}