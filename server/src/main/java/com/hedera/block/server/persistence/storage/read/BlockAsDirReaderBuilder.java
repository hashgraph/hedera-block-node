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

package com.hedera.block.server.persistence.storage.read;

import static com.hedera.block.common.utils.FileUtilities.DEFAULT_DIR_PERMISSIONS;

import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.hapi.block.stream.Block;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.Set;

/**
 * Use builder methods to create a {@link BlockReader} to read blocks from storage.
 *
 * <p>When a block reader is created, it will provide access to read blocks from storage.
 */
public final class BlockAsDirReaderBuilder {
    private final PersistenceStorageConfig config;
    private FileAttribute<Set<PosixFilePermission>> filePerms = DEFAULT_DIR_PERMISSIONS;

    private BlockAsDirReaderBuilder(@NonNull final PersistenceStorageConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    /**
     * Creates a new block reader builder using the minimum required parameters.
     *
     * @param config is required to supply pertinent configuration info for the block reader to
     *     access storage.
     * @return a block reader builder configured with required parameters.
     */
    @NonNull
    public static BlockAsDirReaderBuilder newBuilder(
            @NonNull final PersistenceStorageConfig config) {
        return new BlockAsDirReaderBuilder(config);
    }

    /**
     * Optionally, provide file permissions for the block reader to use when managing block files
     * and directories.
     *
     * @param filePerms the file permissions to use when managing block files and directories.
     * @return a block reader builder configured with required parameters.
     */
    @NonNull
    public BlockAsDirReaderBuilder filePerms(
            @NonNull final FileAttribute<Set<PosixFilePermission>> filePerms) {
        this.filePerms = Objects.requireNonNull(filePerms);
        return this;
    }

    /**
     * Use the build method to construct a block reader to read blocks from storage.
     *
     * @return a new block reader configured with the parameters provided to the builder.
     */
    @NonNull
    public BlockReader<Block> build() {
        return new BlockAsDirReader(config, filePerms);
    }
}
