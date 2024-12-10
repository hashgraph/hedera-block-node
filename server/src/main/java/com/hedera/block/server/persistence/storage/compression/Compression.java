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
import java.nio.file.Path;

/**
 * A compression abstractions that allows for the compression of bytes using
 * different compression algorithms based on specific implementation.
 */
public interface Compression {
    /**
     * This method aims to create a new {@link OutputStream} instance that will
     * run the input data through a compression algorithm (algorithm is based on
     * specific implementation) before writing it to the input path.
     *
     * @param pathToFile valid {@code non-null} {@link Path} instance that will
     * be used to create the resulting {@link OutputStream} instance
     * @return a newly created, fully initialized and valid, {@code non-null}
     * {@link OutputStream} instance that will run the input data through a
     * compression algorithm before writing it to the file based on implementation
     * @throws IOException if an I/O exception occurs
     */
    @NonNull
    OutputStream newCompressingOutputStream(@NonNull final Path pathToFile) throws IOException;
}
