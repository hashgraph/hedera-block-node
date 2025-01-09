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

package com.hedera.block.tools.commands.record2blocks.model;

import static com.hedera.block.tools.commands.record2blocks.util.RecordFileDates.blockTimeInstantToLong;
import static com.hedera.block.tools.commands.record2blocks.util.RecordFileDates.extractRecordFileTime;

import com.hedera.block.tools.commands.record2blocks.gcp.MainNetBucket;
import java.io.InputStream;
import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Represents a file in the record stream blockchain.
 *
 * @param kind the kind of file
 * @param nodeAccountId the node account ID
 * @param path the path to the file in bucket
 * @param blockTime the block time
 * @param size the size of the file
 * @param md5 the MD5 hash of the file
 * @param sidecarIndex the sidecar index, if this file is a sidecar file
 */
public record ChainFile(
        Kind kind, int nodeAccountId, String path, long blockTime, int size, String md5, int sidecarIndex)
        implements Serializable {
    /** The pattern for sidecar file numbers. */
    private static final Pattern SIDECAR_NUMBER_PATTERN =
            Pattern.compile("sidecar/\\d{4}-\\d{2}-\\d{2}T\\d{2}_\\d{2}_\\d{2}\\.\\d{9}Z_(\\d{2})\\.rcd\\.gz");

    /**
     * Creates a new chain file.
     *
     * @param nodeAccountId the node account ID
     * @param path the path to the file in bucket
     * @param size the size of the file
     * @param md5  the MD5 hash of the file
     */
    public ChainFile(int nodeAccountId, String path, int size, String md5) {
        this(
                Kind.fromFilePath(path),
                nodeAccountId,
                path,
                blockTimeInstantToLong(extractRecordFileTime(path.substring(path.lastIndexOf('/') + 1))),
                size,
                md5,
                extractSidecarIndex(path));
    }

    /**
     * Extracts the sidecar index from the file path. If the file is not a sidecar file, returns -1.
     * <p>
     *     Example: <code>https://storage.googleapis.com/hedera-mainnet-streams/recordstreams/record0.0.34/sidecar/2024-04-04T18_03_26.007683847Z_01.rcd.gz</code>
     *
     * @param filePath the file path
     * @return the sidecar index
     */
    private static int extractSidecarIndex(String filePath) {
        return SIDECAR_NUMBER_PATTERN
                .matcher(filePath)
                .results()
                .map(m -> Integer.parseInt(m.group(1)))
                .findFirst()
                .orElse(-1);
    }

    /**
     * Downloads the file from the bucket.
     *
     * @param mainNetBucket the main net bucket that contains the file
     * @return the file as a byte array
     */
    public byte[] download(MainNetBucket mainNetBucket) {
        return mainNetBucket.download(path);
    }

    /**
     * Downloads the file from the bucket as a stream.
     *
     * @param mainNetBucket the main net bucket that contains the file
     * @return the file as a stream
     */
    public InputStream downloadStreaming(MainNetBucket mainNetBucket) {
        return mainNetBucket.downloadStreaming(path);
    }

    /**
     * Enum for the kind of file.
     */
    public enum Kind {
        RECORD,
        SIGNATURE,
        SIDECAR;

        public static Kind fromFilePath(String filePath) {
            if (filePath.contains("sidecar")) {
                return SIDECAR;
            } else if (filePath.endsWith(".rcd") || filePath.endsWith(".rcd.gz")) {
                return RECORD;
            } else if (filePath.endsWith(".rcd_sig")) {
                return SIGNATURE;
            } else {
                throw new IllegalArgumentException("Unknown file type: " + filePath);
            }
        }
    }
}
