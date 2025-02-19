// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.archive;

import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

/**
 * TODO: add documentation
 */
public final class BlockAsLocalFileArchiver implements LocalBlockArchiver {
    private final CompletionService<Void> completionService;
    private final int archiveGroupSize;
    private final AsyncLocalBlockArchiverFactory archiverFactory;

    public BlockAsLocalFileArchiver(
            @NonNull final PersistenceStorageConfig config,
            @NonNull final AsyncLocalBlockArchiverFactory archiverFactory,
            @NonNull final Executor executor) {
        this.completionService = new ExecutorCompletionService<>(executor);
        this.archiveGroupSize = config.archiveBatchSize(); //
        this.archiverFactory = Objects.requireNonNull(archiverFactory);
    }

    @Override
    public void signalThresholdPassed(final long blockNumber) {
        final boolean validThresholdPassed = blockNumber % archiveGroupSize == 0;
        final boolean canArchive = blockNumber - archiveGroupSize * 2L >= 0;
        if (validThresholdPassed && canArchive) {
            // here we need to archive everything below one order of magnitude of the threshold passed
            final AsyncLocalBlockArchiver archiver = archiverFactory.create(blockNumber - archiveGroupSize);
            completionService.submit(archiver, null);
        }
        handleSubmittedResults();
    }

    private void handleSubmittedResults() {
        Future<Void> completionResult;
        while ((completionResult = completionService.poll()) != null) {
            try {
                if (completionResult.isCancelled()) {
                    // todo
                } else {
                    // we call get here to verify that the task has run to completion
                    // we do not expect it to throw an exception, but to publish
                    // a meaningful result, if an exception is thrown, it should be
                    // either considered a bug or an unhandled exception
                    completionResult.get();
                }
            } catch (final ExecutionException e) {
                // todo
                new RuntimeException(e.getCause());
            } catch (final InterruptedException e) {
                // todo What shall we do here? How to handle?
                Thread.currentThread().interrupt();
            }
        }
    }
}
