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

package com.hedera.block.server.persistence.storage.compression;

import com.github.luben.zstd.ZstdOutputStream;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An implementation of {@link Compression} that compresses the data using the
 * Zstandard (Zstd) compression algorithm.
 */
public class ZstdCompression implements Compression {
    private final int compressionLevel;

    /**
     * Constructor.
     *
     * @param config the {@link PersistenceStorageConfig} instance that provides
     * the configuration for the compression algorithm
     */
    private ZstdCompression(@NonNull final PersistenceStorageConfig config) {
        this.compressionLevel = config.compressionLevel();
    }

    /**
     * Factory method. Returns a new, fully initialized instance of
     * {@link ZstdCompression}.
     *
     * @param config the {@link PersistenceStorageConfig} instance that provides
     * the configuration for the compression algorithm
     * @return a new, fully initialized and valid instance of
     * {@link ZstdCompression}
     */
    @NonNull
    public static ZstdCompression of(@NonNull final PersistenceStorageConfig config) {
        return new ZstdCompression(config);
    }

    /**
     * This implementation uses the compression
     * algorithm, but simply generates a stream that writes the data to it`s
     * destination, as it is received.
     * @see Compression#wrap(OutputStream) for API contract
     */
    @NonNull
    @Override
    public OutputStream wrap(@NonNull final OutputStream streamToWrap) throws IOException {
        return new ZstdOutputStream(Objects.requireNonNull(streamToWrap), compressionLevel);
    }

    @NonNull
    @Override
    public String getCompressionFileExtension() {
        return ".zstd";
    }
}
