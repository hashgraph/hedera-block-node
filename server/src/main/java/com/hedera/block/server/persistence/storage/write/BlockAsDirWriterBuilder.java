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

import static com.hedera.block.protos.BlockStreamService.BlockItem;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.persistence.storage.Util;
import com.hedera.block.server.persistence.storage.remove.BlockAsDirRemover;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.config.Config;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Use builder methods to create a {@link BlockWriter} to write blocks to storage.
 *
 * <p>When a block writer is created, it will provide access to write blocks to storage.
 */
public class BlockAsDirWriterBuilder {

    private final String key;
    private final Config config;
    private final BlockNodeContext blockNodeContext;
    private FileAttribute<Set<PosixFilePermission>> filePerms = Util.defaultPerms;
    private BlockRemover blockRemover;

    private BlockAsDirWriterBuilder(
            @NonNull final String key,
            @NonNull final Config config,
            @NonNull final BlockNodeContext blockNodeContext) {
        this.key = key;
        this.config = config;
        this.blockNodeContext = blockNodeContext;
        this.blockRemover =
                new BlockAsDirRemover(Path.of(config.get(key).asString().get()), Util.defaultPerms);
    }

    /**
     * Creates a new block writer builder using the minimum required parameters.
     *
     * @param key is required to read pertinent configuration info.
     * @param config is required to supply pertinent configuration info for the block writer to
     *     access storage.
     * @param blockNodeContext is required to provide metrics reporting mechanisms .
     * @return a block writer builder configured with required parameters.
     */
    @NonNull
    public static BlockAsDirWriterBuilder newBuilder(
            @NonNull final String key,
            @NonNull final Config config,
            @NonNull final BlockNodeContext blockNodeContext) {

        return new BlockAsDirWriterBuilder(key, config, blockNodeContext);
    }

    /**
     * Optionally, provide file permissions for the block writer to use when managing block files
     * and directories.
     *
     * <p>By default, the block writer will use the permissions defined in {@link
     * Util#defaultPerms}. This method is primarily used for testing purposes. Default values should
     * be sufficient for production use.
     *
     * @param filePerms the file permissions to use when managing block files and directories.
     * @return a block writer builder configured with required parameters.
     */
    @NonNull
    public BlockAsDirWriterBuilder filePerms(
            @NonNull FileAttribute<Set<PosixFilePermission>> filePerms) {
        this.filePerms = filePerms;
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
    public BlockAsDirWriterBuilder blockRemover(@NonNull BlockRemover blockRemover) {
        this.blockRemover = blockRemover;
        return this;
    }

    /**
     * Use the build method to construct a block writer to write blocks to storage.
     *
     * @return a new block writer configured with the parameters provided to the builder.
     * @throws IOException when an error occurs while persisting block items to storage.
     */
    @NonNull
    public BlockWriter<BlockItem> build() throws IOException {
        return new BlockAsDirWriter(key, config, blockRemover, filePerms, blockNodeContext);
    }
}
