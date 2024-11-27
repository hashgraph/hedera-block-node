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

package com.hedera.block.server.persistence;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.StorageType;
import com.hedera.block.server.persistence.storage.path.BlockAsLocalDirPathResolver;
import com.hedera.block.server.persistence.storage.path.BlockAsLocalFilePathResolver;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import com.hedera.block.server.persistence.storage.path.NoOpBlockPathResolver;
import com.hedera.block.server.persistence.storage.read.BlockAsLocalDirReader;
import com.hedera.block.server.persistence.storage.read.BlockAsLocalFileReader;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.read.NoOpBlockReader;
import com.hedera.block.server.persistence.storage.remove.BlockAsLocalDirRemover;
import com.hedera.block.server.persistence.storage.remove.BlockAsLocalFileRemover;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.persistence.storage.remove.NoOpRemover;
import com.hedera.block.server.persistence.storage.write.BlockAsLocalDirWriter;
import com.hedera.block.server.persistence.storage.write.BlockAsLocalFileWriter;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.persistence.storage.write.NoOpBlockWriter;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.inject.Singleton;

/** A Dagger module for providing dependencies for Persistence Module. */
@Module
public interface PersistenceInjectionModule {
    /**
     * Provides a block writer singleton using the block node context.
     *
     * @param blockNodeContext the application context
     * @param blockRemover the block remover
     * @param blockPathResolver the block path resolver
     * @return a block writer singleton
     */
    @Provides
    @Singleton
    static BlockWriter<List<BlockItemUnparsed>> providesBlockWriter(
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final BlockRemover blockRemover,
            @NonNull final BlockPathResolver blockPathResolver) {
        Objects.requireNonNull(blockRemover);
        Objects.requireNonNull(blockPathResolver);
        final StorageType persistenceType = blockNodeContext
                .configuration()
                .getConfigData(PersistenceStorageConfig.class)
                .type();
        try {
            return switch (persistenceType) {
                case BLOCK_AS_LOCAL_FILE -> BlockAsLocalFileWriter.of(
                        blockNodeContext, blockRemover, blockPathResolver);
                case BLOCK_AS_LOCAL_DIRECTORY -> BlockAsLocalDirWriter.of(
                        blockNodeContext, blockRemover, blockPathResolver, null);
                case NO_OP -> NoOpBlockWriter.newInstance();
            };
        } catch (final IOException e) {
            // we cannot have checked exceptions with dagger @Provides
            throw new UncheckedIOException("Failed to create BlockWriter", e);
        }
    }

    /**
     * Provides a block reader singleton using the persistence storage config.
     *
     * @param config the persistence storage configuration needed to build the
     * block reader
     * @return a block reader singleton
     */
    @Provides
    @Singleton
    static BlockReader<BlockUnparsed> providesBlockReader(@NonNull final PersistenceStorageConfig config) {
        final StorageType persistenceType = config.type();
        return switch (persistenceType) {
            case BLOCK_AS_LOCAL_FILE -> BlockAsLocalFileReader.newInstance();
            case BLOCK_AS_LOCAL_DIRECTORY -> BlockAsLocalDirReader.of(config, null);
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
            case BLOCK_AS_LOCAL_FILE -> BlockAsLocalFileRemover.newInstance();
            case BLOCK_AS_LOCAL_DIRECTORY -> BlockAsLocalDirRemover.of(blockPathResolver);
            case NO_OP -> NoOpRemover.newInstance();
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
        final Path blockStorageRoot = Path.of(config.liveRootPath());
        return switch (persistenceType) {
            case BLOCK_AS_LOCAL_FILE -> BlockAsLocalFilePathResolver.of(blockStorageRoot);
            case BLOCK_AS_LOCAL_DIRECTORY -> BlockAsLocalDirPathResolver.of(blockStorageRoot);
            case NO_OP -> NoOpBlockPathResolver.newInstance();
        };
    }

    /**
     * Binds the block node event handler to the stream persistence handler.
     *
     * @param streamPersistenceHandler the stream persistence handler
     * @return the block node event handler
     */
    @Binds
    @Singleton
    BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> bindBlockNodeEventHandler(
            @NonNull final StreamPersistenceHandlerImpl streamPersistenceHandler);
}
