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
 * An abstract implementation of {@link Compression} that provides common
 * functionality for all compression implementations. Used also to enforce
 * compression API contract preconditions.
 */
abstract class AbstractCompression implements Compression {
    /**
     * Factory method, used to create the result for the
     * {@link Compression#newCompressingOutputStream(Path)} method.
     */
    @NonNull
    protected abstract OutputStream createNewCompressingOutputStream(@NonNull final Path pathToFile) throws IOException;

    @NonNull
    @Override
    public final OutputStream newCompressingOutputStream(@NonNull final Path pathToFile) throws IOException {
        final String errorMessage;
        if (Files.isDirectory(pathToFile)) {
            errorMessage = "The input path [%s] must not be a directory!".formatted(pathToFile);
        } else if (Files.notExists(pathToFile.getParent())) {
            errorMessage = "The path to the parent directory of the input path [%s] must exist!".formatted(pathToFile);
        } else {
            return createNewCompressingOutputStream(pathToFile);
        }
        throw new IllegalArgumentException(errorMessage);
    }
}
