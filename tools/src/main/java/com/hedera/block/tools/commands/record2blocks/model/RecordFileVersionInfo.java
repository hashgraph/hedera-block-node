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
 * Represents the version & block hash information of a record file.
 *
 * @param hapiProtoVersion the HAPI protocol version
 * @param blockHash the block hash
 */
public record RecordFileVersionInfo (
        SemanticVersion hapiProtoVersion,
        Bytes blockHash
) {
    /* The length of the header in a v2 record file */
    private static final int  V2_HEADER_LENGTH = Integer.BYTES + Integer.BYTES + 1 + 48;

    /**
     * Parses the record file to extract the HAPI protocol version and the block hash.
     *
     * @param recordFile the record file bytes to parse
     * @return the record file version info
     */
    public static RecordFileVersionInfo parse(byte[] recordFile) {
        try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(recordFile))) {
            final int recordFormatVersion = in.readInt();
            return switch (recordFormatVersion) {
                case 2  -> {
                    final int hapiMajorVersion = in.readInt();
                    final SemanticVersion hapiProtoVersion = new SemanticVersion(
                            hapiMajorVersion, 0, 0, null, null);
                    // The hash for v2 files is the hash(header, hash(content)) this is different to other versions
                    MessageDigest digest = MessageDigest.getInstance("SHA-384");
                    digest.update(recordFile, V2_HEADER_LENGTH, recordFile.length - V2_HEADER_LENGTH);
                    final byte[] contentHash = digest.digest();
                    digest.update(recordFile, 0, V2_HEADER_LENGTH);
                    digest.update(contentHash);
                    yield new RecordFileVersionInfo(
                            hapiProtoVersion,
                            Bytes.wrap(digest.digest())
                    );
                }
                case 5 -> {
                    final int hapiMajorVersion = in.readInt();
                    final int hapiMinorVersion = in.readInt();
                    final int hapiPatchVersion = in.readInt();
                    final SemanticVersion hapiProtoVersion = new SemanticVersion(
                            hapiMajorVersion, hapiMinorVersion, hapiPatchVersion, null, null);
                    // skip to last hash object
                    in.skipBytes(in.available() - HASH_OBJECT_SIZE_BYTES);
                    final byte[] endHashObject = readHashObject(in);
                    yield new RecordFileVersionInfo(
                            hapiProtoVersion,
                            Bytes.wrap(endHashObject)
                    );
                }
                case 6 -> {
                    final RecordStreamFile recordStreamFile = RecordStreamFile.PROTOBUF.parse(new ReadableStreamingData(
                            in));
                    yield new RecordFileVersionInfo(
                            recordStreamFile.hapiProtoVersion(),
                            recordStreamFile.endObjectRunningHash().hash()
                    );
                }
                default ->
                    throw new UnsupportedOperationException("Unsupported record format version: " + recordFormatVersion);
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}