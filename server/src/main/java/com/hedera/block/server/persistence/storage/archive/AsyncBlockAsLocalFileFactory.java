// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.archive;

import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;

/**
 * TODO: add documentation
 */
public class AsyncBlockAsLocalFileFactory implements AsyncLocalBlockArchiverFactory {
    private final PersistenceStorageConfig config;
    private final BlockPathResolver pathResolver;

    public AsyncBlockAsLocalFileFactory(
            @NonNull final PersistenceStorageConfig config, @NonNull final BlockPathResolver pathResolver) {
        this.config = Objects.requireNonNull(config);
        this.pathResolver = Objects.requireNonNull(pathResolver);
    }

    @Override
    public AsyncLocalBlockArchiver create(final long blockNumber) {
        return new AsyncBlockAsLocalFileArchiver(blockNumber, config, pathResolver);
    }
}
