package com.hedera.block.tools.commands.record2blocks.model;

import static com.hedera.block.tools.commands.record2blocks.util.RecordFileDates.blockTimeInstantToLong;
import static com.hedera.block.tools.commands.record2blocks.util.RecordFileDates.extractRecordFileTime;

import com.hedera.block.tools.commands.record2blocks.gcp.MainNetBucket;
import java.io.Serializable;

public record ChainFile(
        Kind kind,
        int nodeAccountId,
        String path,
        long blockTime,
        int size,
        String md5) implements Serializable {

    public ChainFile(int nodeAccountId, String path, int size, String md5) {
        this(Kind.fromFilePath(path), nodeAccountId, path,
                blockTimeInstantToLong(extractRecordFileTime(path.substring(path.lastIndexOf('/') + 1))),
                size, md5);
    }

    public byte[] download(MainNetBucket mainNetBucket) {
        return mainNetBucket.download(path);
    }

    enum Kind {
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
