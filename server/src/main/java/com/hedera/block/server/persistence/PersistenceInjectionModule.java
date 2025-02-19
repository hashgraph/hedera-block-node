// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence;

import com.hedera.block.server.ack.AckHandler;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.CompressionType;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.StorageType;
import com.hedera.block.server.persistence.storage.archive.AsyncBlockAsLocalFileFactory;
import com.hedera.block.server.persistence.storage.archive.AsyncLocalBlockArchiverFactory;
import com.hedera.block.server.persistence.storage.archive.BlockAsLocalFileArchiver;
import com.hedera.block.server.persistence.storage.archive.LocalBlockArchiver;
import com.hedera.block.server.persistence.storage.compression.Compression;
import com.hedera.block.server.persistence.storage.compression.NoOpCompression;
import com.hedera.block.server.persistence.storage.compression.ZstdCompression;
import com.hedera.block.server.persistence.storage.path.BlockAsLocalFilePathResolver;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.block.server.persistence.storage.path.NoOpBlockPathResolver;
import com.hedera.block.server.persistence.storage.read.BlockAsLocalFileReader;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.read.NoOpBlockReader;
import com.hedera.block.server.persistence.storage.remove.BlockAsLocalFileRemover;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.persistence.storage.remove.NoOpBlockRemover;
import com.hedera.block.server.persistence.storage.write.AsyncBlockAsLocalFileWriterFactory;
import com.hedera.block.server.persistence.storage.write.AsyncBlockWriterFactory;
import com.hedera.block.server.persistence.storage.write.AsyncNoOpWriterFactory;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import javax.inject.Singleton;

/** A Dagger module for providing dependencies for Persistence Module. */
@Module
public interface PersistenceInjectionModule {
    /**
     * Provides an async block writer factory singleton using the persistence
     * storage config.
     *
     * @param config the persistence storage config needed to discern the type
     * of the async block writer factory
     * @param blockPathResolver the block path resolver
     * @param compression the compression used
     * @return an async block writer factory singleton
     */
    @Provides
    @Singleton
    static AsyncBlockWriterFactory providesAsyncBlockWriterFactory(
            @NonNull final PersistenceStorageConfig config,
            @NonNull final BlockPathResolver blockPathResolver,
            @NonNull final BlockRemover blockRemover,
            @NonNull final Compression compression,
            @NonNull final AckHandler ackHandler,
            @NonNull final BlockNodeContext context) {
        final StorageType type = config.type();
        return switch (type) {
            case BLOCK_AS_LOCAL_FILE -> new AsyncBlockAsLocalFileWriterFactory(
                    blockPathResolver, blockRemover, compression, ackHandler, context.metricsService());
            case NO_OP -> new AsyncNoOpWriterFactory(ackHandler, context.metricsService());
        };
    }

    /**
     * Provides a block reader singleton using the persistence storage config.
     *
     * @param config the persistence storage configuration needed to build the
     * block reader
     * @param blockPathResolver the block path resolver needed to build
     * the block reader
     * @return a block reader singleton
     */
    @Provides
    @Singleton
    static BlockReader<BlockUnparsed> providesBlockReader(
            @NonNull final PersistenceStorageConfig config,
            @NonNull final BlockPathResolver blockPathResolver,
            @NonNull final Compression compression) {
        final StorageType persistenceType = config.type();
        return switch (persistenceType) {
            case BLOCK_AS_LOCAL_FILE -> BlockAsLocalFileReader.of(compression, blockPathResolver);
            case NO_OP -> NoOpBlockReader.newInstance();
        };
    }

    /**
     * Provides a block remover singleton using the persistence storage config.
     *
     * @param config the persistence storage configuration needed to build the
     * block remover
     * @param blockPathResolver the block path resolver
     * @return a block remover singleton
     */
    @Provides
    @Singleton
    static BlockRemover providesBlockRemover(
            @NonNull final PersistenceStorageConfig config, @NonNull final BlockPathResolver blockPathResolver) {
        Objects.requireNonNull(blockPathResolver);
        final StorageType persistenceType = config.type();
        return switch (persistenceType) {
            case BLOCK_AS_LOCAL_FILE -> new BlockAsLocalFileRemover(blockPathResolver);
            case NO_OP -> NoOpBlockRemover.newInstance();
        };
    }

    /**
     * Provides a path resolver singleton using the persistence storage config.
     *
     * @param config the persistence storage configuration needed to build the
     * path resolver
     * @return a path resolver singleton
     */
    @Provides
    @Singleton
    static BlockPathResolver providesPathResolver(@NonNull final PersistenceStorageConfig config) {
        final StorageType persistenceType = config.type();
        return switch (persistenceType) {
            case BLOCK_AS_LOCAL_FILE -> BlockAsLocalFilePathResolver.of(config);
            case NO_OP -> NoOpBlockPathResolver.newInstance();
        };
    }

    /**
     * Provides a compression singleton using the persistence config.
     *
     * @param config the persistence storage configuration needed to build the
     * compression
     * @return a compression singleton
     */
    @Provides
    @Singleton
    static Compression providesCompression(@NonNull final PersistenceStorageConfig config) {
        final CompressionType compressionType = config.compression();
        return switch (compressionType) {
            case ZSTD -> ZstdCompression.of(config);
            case NONE -> NoOpCompression.newInstance();
        };
    }

    @Provides
    @Singleton
    static AsyncLocalBlockArchiverFactory providesAsyncLocalBlockArchiverFactory(
            @NonNull final PersistenceStorageConfig config, @NonNull final BlockPathResolver blockPathResolver) {
        final StorageType persistenceType = config.type();
        return switch (persistenceType) {
            case BLOCK_AS_LOCAL_FILE -> new AsyncBlockAsLocalFileFactory(config, blockPathResolver);
            case NO_OP -> throw new UnsupportedOperationException(); // todo implement
        };
    }

    @Provides
    @Singleton
    static LocalBlockArchiver providesLocalBlockArchiver(
            @NonNull final PersistenceStorageConfig config,
            @NonNull final AsyncLocalBlockArchiverFactory asyncLocalBlockArchiverFactory) {
        final StorageType persistenceType = config.type();
        return switch (persistenceType) {
            case BLOCK_AS_LOCAL_FILE -> new BlockAsLocalFileArchiver(
                    config, asyncLocalBlockArchiverFactory, Executors.newFixedThreadPool(10));
            case NO_OP -> throw new UnsupportedOperationException();
        };
    }

    /**
     * Provides a block node event handler singleton (stream persistence handler)
     * @param subscriptionHandler the subscription handler
     * @param notifier the notifier
     * @param blockNodeContext the block node context
     * @param serviceStatus the service status
     * @param ackHandler the ack handler
     * @param asyncBlockWriterFactory the async block writer factory
     * @return the persistence block node event handler singleton
     */
    @Provides
    @Singleton
    static BlockNodeEventHandler<ObjectEvent<List<BlockItemUnparsed>>> providesBlockNodeEventHandler(
            @NonNull final SubscriptionHandler<List<BlockItemUnparsed>> subscriptionHandler,
            @NonNull final Notifier notifier,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final AckHandler ackHandler,
            @NonNull final AsyncBlockWriterFactory asyncBlockWriterFactory,
            @NonNull final PersistenceStorageConfig persistenceStorageConfig,
            @NonNull final LocalBlockArchiver localBlockArchiver) {
        return new StreamPersistenceHandlerImpl(
                subscriptionHandler,
                notifier,
                blockNodeContext,
                serviceStatus,
                ackHandler,
                asyncBlockWriterFactory,
                Executors.newFixedThreadPool(5),
                localBlockArchiver,
                persistenceStorageConfig);
    }
}
