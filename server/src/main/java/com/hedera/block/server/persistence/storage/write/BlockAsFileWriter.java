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

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.path.PathResolver;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * TODO: add documentation
 */
class BlockAsFileWriter extends AbstractBlockWriter<List<BlockItem>> {
    private Block curentBlock; // fixme this is temporary just to explore the workflow and make proof of concept

    BlockAsFileWriter(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final BlockRemover blockRemover,
            @NonNull final PathResolver pathResolver) {
        super(blockNodeContext.metricsService(), blockRemover, pathResolver);
    }

    @Override
    public Optional<List<BlockItem>> write(@NonNull final List<BlockItem> toWrite) throws IOException {
        if (toWrite.getFirst().hasBlockHeader()) {
            curentBlock = Block.newBuilder().items(toWrite).build();
        } else {
            final List<BlockItem> currentItems = curentBlock.items();
            currentItems.addAll(toWrite);
            curentBlock = Block.newBuilder().items(currentItems).build();
        }

        if (toWrite.getLast().hasBlockProof()) {
            return writeToFs(curentBlock);
        } else {
            return Optional.empty();
        }
    }

    private Optional<List<BlockItem>> writeToFs(final Block blockToWrite) throws IOException {
        final long number = blockToWrite.items().getFirst().blockHeader().number(); // fixme could be null, handle!

        // todo we should handle cases where the block path exists, we do not expect it to
        // exist at this stage, if it does, there is something wrong here
        final Path blockToWritePathResolved = pathResolver.resolvePathToBlock(number);
        Files.createDirectories(blockToWritePathResolved.getParent());
        Files.createFile(blockToWritePathResolved);

        try (final FileOutputStream fos = new FileOutputStream(blockToWritePathResolved.toFile())) {
            Block.PROTOBUF.toBytes(blockToWrite).writeTo(fos);
            // todo what should be fallback logic if something goes wrong here? we attempt to resolve the path
            // with proper perms? we
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
        curentBlock = null;
        return Optional.of(blockToWrite.items());
    }
}
