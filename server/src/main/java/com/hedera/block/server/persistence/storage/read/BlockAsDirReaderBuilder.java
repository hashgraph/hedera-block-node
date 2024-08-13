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

import com.hedera.hapi.block.stream.Block;

import com.hedera.block.server.persistence.storage.FileUtils;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Use builder methods to create a {@link BlockReader} to read blocks from storage.
 *
 * <p>When a block reader is created, it will provide access to read blocks from storage.
 */
public class BlockAsDirReaderBuilder {

    private FileAttribute<Set<PosixFilePermission>> filePerms = FileUtils.defaultPerms;
    private final PersistenceStorageConfig config;

    private BlockAsDirReaderBuilder(@NonNull PersistenceStorageConfig config) {
        this.config = config;
    }

    /**
     * Creates a new block reader builder using the minimum required parameters.
     *
     * @param config is required to supply pertinent configuration info for the block reader to
     *     access storage.
     * @return a block reader builder configured with required parameters.
     */
    @NonNull
    public static BlockAsDirReaderBuilder newBuilder(@NonNull PersistenceStorageConfig config) {
        return new BlockAsDirReaderBuilder(config);
    }

    /**
     * Optionally, provide file permissions for the block reader to use when managing block files
     * and directories.
     *
     * <p>By default, the block reader will use the permissions defined in {@link
     * FileUtils#defaultPerms}. This method is primarily used for testing purposes. Default values
     * should be sufficient for production use.
     *
     * @param filePerms the file permissions to use when managing block files and directories.
     * @return a block reader builder configured with required parameters.
     */
    @NonNull
    public BlockAsDirReaderBuilder filePerms(
            @NonNull final FileAttribute<Set<PosixFilePermission>> filePerms) {
        this.filePerms = filePerms;
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
