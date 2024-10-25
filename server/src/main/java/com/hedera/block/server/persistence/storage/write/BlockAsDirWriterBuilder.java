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

import static com.hedera.block.common.utils.FileUtilities.DEFAULT_DIR_PERMISSIONS;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.remove.BlockAsDirRemover;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Use builder methods to create a {@link BlockWriter} to write blocks to storage.
 *
 * <p>When a block writer is created, it will provide access to write blocks to storage.
 */
public final class BlockAsDirWriterBuilder {
    private final BlockNodeContext blockNodeContext;
    private FileAttribute<Set<PosixFilePermission>> filePerms = DEFAULT_DIR_PERMISSIONS;
    private BlockRemover blockRemover;

    private BlockAsDirWriterBuilder(@NonNull final BlockNodeContext blockNodeContext) {
        this.blockNodeContext = Objects.requireNonNull(blockNodeContext);
        final PersistenceStorageConfig config =
                blockNodeContext.configuration().getConfigData(PersistenceStorageConfig.class);
        this.blockRemover =
                new BlockAsDirRemover(Path.of(config.rootPath()), DEFAULT_DIR_PERMISSIONS);
    }

    /**
     * Creates a new block writer builder using the minimum required parameters.
     *
     * @param blockNodeContext is required to provide metrics reporting mechanisms .
     * @return a block writer builder configured with required parameters.
     */
    @NonNull
    public static BlockAsDirWriterBuilder newBuilder(
            @NonNull final BlockNodeContext blockNodeContext) {
        return new BlockAsDirWriterBuilder(blockNodeContext);
    }

    /**
     * Optionally, provide file permissions for the block writer to use when managing block files
     * and directories.
     *
     * @param filePerms the file permissions to use when managing block files and directories.
     * @return a block writer builder configured with required parameters.
     */
    @NonNull
    public BlockAsDirWriterBuilder filePerms(
            @NonNull final FileAttribute<Set<PosixFilePermission>> filePerms) {
        this.filePerms = Objects.requireNonNull(filePerms);
        return this;
    }

    /**
     * Optionally, provide a block remover to remove blocks from storage.
     *
     * <p>By default, the block writer will use the block remover defined in {@link
     * BlockAsDirRemover}. This method is primarily used for testing purposes. Default values should
     * be sufficient for production use.
     *
     * @param blockRemover the block remover to use when removing blocks from storage.
     * @return a block writer builder configured with required parameters.
     */
    @NonNull
    public BlockAsDirWriterBuilder blockRemover(@NonNull final BlockRemover blockRemover) {
        this.blockRemover = Objects.requireNonNull(blockRemover);
        return this;
    }

    /**
     * Use the build method to construct a block writer to write blocks to storage.
     *
     * @return a new block writer configured with the parameters provided to the builder.
     * @throws IOException when an error occurs while persisting block items to storage.
     */
    @NonNull
    public BlockWriter<List<BlockItem>> build() throws IOException {
        return new BlockAsDirWriter(blockRemover, filePerms, blockNodeContext);
    }
}
