// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A no-op path resolver.
 */
public final class NoOpBlockPathResolver implements BlockPathResolver {
    /**
     * Constructor.
     */
    private NoOpBlockPathResolver() {}

    /**
     * This method creates and returns a new instance of
     * {@link NoOpBlockPathResolver}.
     *
     * @return a new, fully initialized instance of
     * {@link NoOpBlockPathResolver}
     */
    public static NoOpBlockPathResolver newInstance() {
        return new NoOpBlockPathResolver();
    }

    /**
     * No-op resolver. Does nothing and always returns a path under '/tmp' that
     * resolves to 'blockNumber.tmp.blk'. No preconditions check also.
     */
    @NonNull
    @Override
    public Path resolveLiveRawPathToBlock(final long blockNumber) {
        final String blockName = String.format("%d.tmp.blk", blockNumber);
        return Path.of("/tmp/hashgraph/blocknode/data/").resolve(blockName);
    }

    /**
     * No-op resolver. Does nothing and always returns a path under '/tmp' that
     * resolves to 'blockNumber.tmp.blk'. No preconditions check also.
     */
    @NonNull
    @Override
    public Path resolveArchiveRawPathToBlock(final long blockNumber) {
        return resolveLiveRawPathToBlock(blockNumber);
    }

    /**
     * No-op resolver. Does nothing and always returns an empty optional.
     * No preconditions check also.
     */
    @NonNull
    @Override
    public Optional<Path> findBlock(final long blockNumber) {
        return Optional.empty();
    }
}
