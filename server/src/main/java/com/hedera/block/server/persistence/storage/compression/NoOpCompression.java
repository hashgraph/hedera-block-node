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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An implementation of {@link Compression} that does not compress the data. It
 * does not use any algorithm, but simply generates a stream that writes the
 * data to it`s destination, as it is received.
 */
public class NoOpCompression implements Compression {
    /**
     * This method aims to create a new {@link OutputStream} instance that will
     * write the input data to it`s destination without any compression.
     *
     * @param pathToFile valid {@code non-null} {@link Path} instance that will
     * be used to create the resulting {@link OutputStream} instance
     * @return a newly created, fully initialized and valid, {@code non-null}
     * {@link OutputStream} instance that will write the input data to it`s
     * destination without any compression
     * @throws IOException if an I/O exception occurs
     */
    @NonNull
    @Override
    public OutputStream newCompressingOutputStream(@NonNull final Path pathToFile) throws IOException {
        return Files.newOutputStream(pathToFile);
    }

    @NonNull
    @Override
    public String getCompressionFileExtension() {
        return "";
    }
}
