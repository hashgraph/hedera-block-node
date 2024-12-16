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

/**
 * A compression abstractions that allows for the compression of bytes using
 * different compression algorithms based on specific implementation.
 */
public interface Compression {
    /**
     * This method takes a valid, {@code non-null} {@link OutputStream} instance
     * and wraps it with the specific compression algorithm implementation. The
     * resulting {@link OutputStream} is then returned.
     *
     * @param streamToWrap a valid {@code non-null} {@link OutputStream} to wrap
     * @return a {@code non-null} {@link OutputStream} that wraps the provided
     * {@link OutputStream} with the specific compression algorithm
     * implementation
     * @throws IOException if an I/O exception occurs
     */
    @NonNull
    OutputStream wrap(@NonNull final OutputStream streamToWrap) throws IOException;

    /**
     * This method aims to return a valid, {@code non-blank} {@link String} that
     * represents the file extension for the given specific implementation,
     * based on the compression algorithm used.
     *
     * @return a valid, {@code non-blank} {@link String} that represents the
     * file extension for the given specific implementation, based on the
     * compression algorithm used
     */
    @NonNull
    String getCompressionFileExtension();
}
