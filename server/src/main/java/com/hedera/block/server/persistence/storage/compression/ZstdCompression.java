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
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An implementation of {@link Compression} that compresses the data using the
 * Zstandard (Zstd) compression algorithm.
 */
public class ZstdCompression extends AbstractCompression {
    /**
     * This implementation uses the compression
     * algorithm, but simply generates a stream that writes the data to it`s
     * destination, as it is received.
     * @see Compression#newCompressingOutputStream(Path) for API contract
     */
    @NonNull
    @Override
    protected OutputStream createNewCompressingOutputStream(@NonNull final Path pathToFile) throws IOException {
        final Path localPathToFile =
                pathToFile.resolveSibling(pathToFile.getFileName() + getCompressionFileExtension());
        return new ZstdOutputStream(Files.newOutputStream(localPathToFile));
    }

    @NonNull
    @Override
    public String getCompressionFileExtension() {
        return ".zstd";
    }
}