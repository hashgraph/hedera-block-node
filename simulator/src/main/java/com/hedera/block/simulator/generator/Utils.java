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

package com.hedera.block.simulator.generator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/** Utility class for the simulator. */
public class Utils {

    private Utils() {}

    /**
     * Read a GZIP file and return the content as a byte array.
     *
     * @param filePath Path to the GZIP file
     * @return byte array of the content of the GZIP file
     * @throws IOException if an I/O error occurs
     */
    public static byte[] readGzFile(Path filePath) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(filePath);
                GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream)) {
            return gzipInputStream.readAllBytes();
        }
    }
}
