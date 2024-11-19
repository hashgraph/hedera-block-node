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
import com.hedera.block.server.persistence.storage.StorageType;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockAsFileReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.read.NoOpBlockReader;
import com.hedera.block.server.persistence.storage.remove.BlockAsDirRemover;
import com.hedera.block.server.persistence.storage.remove.BlockAsFileRemover;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.persistence.storage.remove.NoOpRemover;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockAsFileWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.persistence.storage.write.NoOpBlockWriter;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
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
     * @return a block writer singleton
     */
    @Provides
    @Singleton
    static BlockWriter<List<BlockItem>> providesBlockWriter(
            @NonNull final BlockNodeContext blockNodeContext, @NonNull final BlockRemover blockRemover) {
        Objects.requireNonNull(blockNodeContext);
        Objects.requireNonNull(blockRemover);
        final StorageType persistenceType = blockNodeContext
                .configuration()
                .getConfigData(PersistenceStorageConfig.class)
                .type();
        try {
            return switch (persistenceType) {
                case null -> throw new NullPointerException(
                        "Persistence StorageType cannot be [null], cannot create an instance of BlockWriter");
                case BLOCK_AS_FILE -> BlockAsFileWriterBuilder.newBuilder(blockNodeContext, blockRemover)
                        .build();
                case BLOCK_AS_DIR -> BlockAsDirWriterBuilder.newBuilder(blockNodeContext, blockRemover)
                        .build();
                case NOOP -> new NoOpBlockWriter(blockNodeContext, blockRemover);
            };
        } catch (final IOException e) {
            throw new RuntimeException("Failed to create BlockWriter", e);
        }
    }

    /**
     * Provides a block reader singleton using the persistence storage config.
     *
     * @param config the persistence storage configuration needed to build the block reader
     * @return a block reader singleton
     */
    @Provides
    @Singleton
    static BlockReader<Block> providesBlockReader(@NonNull final PersistenceStorageConfig config) {
        final StorageType persistenceType = Objects.requireNonNull(config).type();
        return switch (persistenceType) {
            case null -> throw new NullPointerException(
                    "Persistence StorageType cannot be [null], cannot create an instance of BlockWriter");
            case BLOCK_AS_FILE -> BlockAsFileReaderBuilder.newBuilder().build();
            case BLOCK_AS_DIR -> BlockAsDirReaderBuilder.newBuilder(config).build();
            case NOOP -> new NoOpBlockReader();
        };
    }

    /**
     * Provides a block reader singleton using the persistence storage config.
     *
     * @param config the persistence storage configuration needed to build the block reader
     * @return a block reader singleton
     */
    @Provides
    @Singleton
    static BlockRemover providesBlockRemover(@NonNull final PersistenceStorageConfig config) {
        final StorageType persistenceType = Objects.requireNonNull(config).type();
        return switch (persistenceType) {
            case null -> throw new NullPointerException(
                    "Persistence StorageType cannot be [null], cannot create an instance of BlockWriter");
            case BLOCK_AS_FILE -> new BlockAsFileRemover();
            case BLOCK_AS_DIR -> new BlockAsDirRemover(Path.of(config.rootPath()));
            case NOOP -> new NoOpRemover();
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
    BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> bindBlockNodeEventHandler(
            @NonNull final StreamPersistenceHandlerImpl streamPersistenceHandler);
}
