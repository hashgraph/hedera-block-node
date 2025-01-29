// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.CompressionType;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;

/**
 * TODO: add documentation
 */
public record ArchiveBlockPath(
        long blockNumber,
        @NonNull Path dirPath,
        @NonNull String zipFileName,
        @NonNull String zipEntryName,
        @NonNull CompressionType compressionType) {}
