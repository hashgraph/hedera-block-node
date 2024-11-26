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

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A Block writer that handles writing of block-as-file.
 */
class BlockAsFileWriter implements LocalBlockWriter<List<BlockItemUnparsed>> {
    private final MetricsService metricsService;
    private final BlockRemover blockRemover; // todo do I need here?
    private final BlockPathResolver blockPathResolver;
    private BlockUnparsed
            currentBlockUnparsed; // fixme this is temporary just to explore the workflow and make proof of concept

    /**
     * Constructor.
     *
     * @param blockNodeContext valid, {@code non-null} instance of {@link BlockNodeContext} used to
     * get the {@link MetricsService}
     * @param blockRemover valid, {@code non-null} instance of {@link BlockRemover} used to remove
     * blocks in case of cleanup
     * @param blockPathResolver valid, {@code non-null} instance of {@link BlockPathResolver} used
     * to resolve paths to Blocks
     */
    BlockAsFileWriter(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final BlockRemover blockRemover,
            @NonNull final BlockPathResolver blockPathResolver) {
        this.metricsService = Objects.requireNonNull(blockNodeContext.metricsService());
        this.blockRemover = Objects.requireNonNull(blockRemover);
        this.blockPathResolver = Objects.requireNonNull(blockPathResolver);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<List<BlockItemUnparsed>> write(@NonNull final List<BlockItemUnparsed> toWrite)
            throws IOException, ParseException {
        if (toWrite.getFirst().hasBlockHeader()) {
            currentBlockUnparsed =
                    BlockUnparsed.newBuilder().blockItems(toWrite).build();
        } else {
            final List<BlockItemUnparsed> currentItems = currentBlockUnparsed.blockItems();
            currentItems.addAll(toWrite);
            currentBlockUnparsed =
                    BlockUnparsed.newBuilder().blockItems(currentItems).build();
        }

        if (toWrite.getLast().hasBlockProof()) {
            metricsService.get(BlocksPersisted).increment();
            return Optional.ofNullable(writeToFs(currentBlockUnparsed));
        } else {
            return Optional.empty();
        }
    }

    // todo we could recursively retry if exception occurs, then after a few attempts
    // if we cannot persist, we must throw the initial exception
    private List<BlockItemUnparsed> writeToFs(final BlockUnparsed blockToWrite) throws IOException, ParseException {
        final long number = BlockHeader.PROTOBUF
                .parse(blockToWrite.blockItems().getFirst().blockHeader())
                .number(); // fixme could be null, handle!

        // todo we should handle cases where the block path exists, we do not expect it to
        // exist at this stage, if it does, there is something wrong here
        final Path blockToWritePathResolved = blockPathResolver.resolvePathToBlock(number);
        Files.createDirectories(blockToWritePathResolved.getParent());
        Files.createFile(blockToWritePathResolved);

        // todo maybe this is not the place to handle the exceptions, but maybe if we do a retry mechanism we
        // must catch here. also should we repair permissions and use the remover to remove as a cleanup?
        // do we have such cases?
        try (final FileOutputStream fos = new FileOutputStream(blockToWritePathResolved.toFile())) {
            BlockUnparsed.PROTOBUF.toBytes(blockToWrite).writeTo(fos);
            // todo what should be fallback logic if something goes wrong here? we attempt to resolve the path
            // with proper perms (is that necessary)? we must clean up and retry?
        } catch (final IOException ioe) {
            // todo handle properly
            throw new UncheckedIOException(ioe);
        } catch (final UncheckedIOException uioe) {
            // todo handle properly
            throw uioe;
        } catch (final Exception e) {
            // todo handle properly
            throw new RuntimeException(e);
        }
        currentBlockUnparsed = null;
        return blockToWrite.blockItems();
    }
}
