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
import java.io.Serializable;

public record ChainFile(Kind kind, int nodeAccountId, String path, long blockTime, int size, String md5)
        implements Serializable {

    public ChainFile(int nodeAccountId, String path, int size, String md5) {
        this(
                Kind.fromFilePath(path),
                nodeAccountId,
                path,
                blockTimeInstantToLong(extractRecordFileTime(path.substring(path.lastIndexOf('/') + 1))),
                size,
                md5);
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
