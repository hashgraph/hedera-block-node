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

import static com.hedera.block.tools.commands.record2blocks.model.ParsedSignatureFile.HASH_OBJECT_SIZE_BYTES;
import static com.hedera.block.tools.commands.record2blocks.model.ParsedSignatureFile.readHashObject;

import com.hedera.hapi.node.base.SemanticVersion;
import com.hedera.hapi.streams.RecordStreamFile;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.hedera.pbj.runtime.io.stream.ReadableStreamingData;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.security.MessageDigest;

/**
 * Represents the version and block hash information of a record file.
 * <p>
 * The old record file formats are documented in the
 * <a href="https://github.com/search?q=repo%3Ahashgraph%2Fhedera-mirror-node%20%22implements%20RecordFileReader%22&type=code">
 * Mirror Node code.</a> and in the legacy documentation on
 * <a href="http://web.archive.org/web/20221006192449/https://docs.hedera.com/guides/docs/record-and-event-stream-file-formats">
 * Way Back Machine</a>
 * </p>
 *
 * @param hapiProtoVersion the HAPI protocol version
 * @param blockHash the block hash
 * @param recordFileContents the record file contents
 */
public record RecordFileInfo(SemanticVersion hapiProtoVersion, Bytes blockHash, byte[] recordFileContents) {
    /* The length of the header in a v2 record file */
    private static final int V2_HEADER_LENGTH = Integer.BYTES + Integer.BYTES + 1 + 48;

    /**
     * Parses the record file to extract the HAPI protocol version and the block hash.
     *
     * @param recordFile the record file bytes to parse
     * @return the record file version info
     */
    public static RecordFileInfo parse(byte[] recordFile) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(recordFile))) {
            final int recordFormatVersion = in.readInt();
            // This is a minimal parser for all record file formats only extracting the necessary information
            return switch (recordFormatVersion) {
                case 2 -> {
                    final int hapiMajorVersion = in.readInt();
                    final SemanticVersion hapiProtoVersion = new SemanticVersion(hapiMajorVersion, 0, 0, null, null);
                    // The hash for v2 files is the hash(header, hash(content)) this is different to other versions
                    // the block hash is not available in the file so we have to calculate it
                    MessageDigest digest = MessageDigest.getInstance("SHA-384");
                    digest.update(recordFile, V2_HEADER_LENGTH, recordFile.length - V2_HEADER_LENGTH);
                    final byte[] contentHash = digest.digest();
                    digest.update(recordFile, 0, V2_HEADER_LENGTH);
                    digest.update(contentHash);
                    yield new RecordFileInfo(hapiProtoVersion, Bytes.wrap(digest.digest()), recordFile);
                }
                case 5 -> {
                    final int hapiMajorVersion = in.readInt();
                    final int hapiMinorVersion = in.readInt();
                    final int hapiPatchVersion = in.readInt();
                    final SemanticVersion hapiProtoVersion =
                            new SemanticVersion(hapiMajorVersion, hapiMinorVersion, hapiPatchVersion, null, null);
                    // skip to last hash object. This trick allows us to not have to understand the format for record
                    // file items and their contents which is much more complicated. For v5 and v6 the block hash is the
                    // end running hash which is written as a special item at the end of the file.
                    in.skipBytes(in.available() - HASH_OBJECT_SIZE_BYTES);
                    final byte[] endHashObject = readHashObject(in);
                    yield new RecordFileInfo(hapiProtoVersion, Bytes.wrap(endHashObject), recordFile);
                }
                case 6 -> {
                    // V6 is nice and easy as it is all protobuf encoded after the first version integer
                    final RecordStreamFile recordStreamFile =
                            RecordStreamFile.PROTOBUF.parse(new ReadableStreamingData(in));
                    // For v6 the block hash is the end running hash which is accessed via endObjectRunningHash()
                    if (recordStreamFile.endObjectRunningHash() == null) {
                        throw new IllegalStateException("No end object running hash in record file");
                    }
                    yield new RecordFileInfo(
                            recordStreamFile.hapiProtoVersion(),
                            recordStreamFile.endObjectRunningHash().hash(),
                            recordFile);
                }
                default -> throw new UnsupportedOperationException(
                        "Unsupported record format version: " + recordFormatVersion);
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
