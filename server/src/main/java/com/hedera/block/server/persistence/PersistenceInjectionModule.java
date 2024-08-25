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
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.read.BlockAsDirReaderBuilder;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.persistence.storage.write.BlockAsDirWriterBuilder;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
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
    static BlockWriter<BlockItem> providesBlockWriter(@NonNull BlockNodeContext blockNodeContext) {
        try {
            return BlockAsDirWriterBuilder.newBuilder(blockNodeContext).build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create block writer", e);
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
    static BlockReader<Block> providesBlockReader(@NonNull PersistenceStorageConfig config) {
        return BlockAsDirReaderBuilder.newBuilder(config).build();
    }
}
