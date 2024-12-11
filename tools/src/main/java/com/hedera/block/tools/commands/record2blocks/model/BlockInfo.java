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

import com.hedera.block.tools.commands.record2blocks.gcp.MainNetBucket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class BlockInfo {
    private final long blockNum;
    private final long blockTime;
    private final List<ChainFile> recordFiles;
    private final List<ChainFile> signatureFiles;
    private final List<ChainFile> sidecarFiles;
    private Map.Entry<String, Long> mostCommonRecordFileMd5EntryAndCount;
    private ChainFile mostCommonRecordFile;
    private Map.Entry<String, Long> mostCommonSidecarFileMd5EntryAndCount;
    private ChainFile mostCommonSideCarFile;

    public BlockInfo(long blockNum, long blockTime, int minNodeAccountId, int maxNodeAccountId) {
        this.blockNum = blockNum;
        this.blockTime = blockTime;
        final int nodeCount = maxNodeAccountId - minNodeAccountId;
        this.recordFiles = new ArrayList<>(nodeCount);
        this.signatureFiles = new ArrayList<>(nodeCount);
        this.sidecarFiles = new ArrayList<>(nodeCount);
    }

    public long blockNum() {
        return blockNum;
    }

    public long blockTime() {
        return blockTime;
    }

    public List<ChainFile> recordFiles() {
        return recordFiles;
    }

    public List<ChainFile> sidecarFiles() {
        return sidecarFiles;
    }

    /**
     * Collect chain files into the appropriate list.
     */
    public void addChainFile(ChainFile chainFile) {
        switch (chainFile.kind()) {
            case RECORD -> recordFiles.add(chainFile);
            case SIGNATURE -> signatureFiles.add(chainFile);
            case SIDECAR -> sidecarFiles.add(chainFile);
        }
    }

    /**
     * Find the most common md5 hash in all record files and signature files.
     */
    public void finishAddingFiles() {
        // find most common md5 hash in all record files
        final Map<String, Long> md5Counts =
                recordFiles.stream().collect(Collectors.groupingBy(ChainFile::md5, Collectors.counting()));
        mostCommonRecordFileMd5EntryAndCount =
                md5Counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (mostCommonRecordFileMd5EntryAndCount == null) {
            throw new IllegalStateException("No record files found");
        }
        // find the first record file with the most common md5 hash
        mostCommonRecordFile = recordFiles.stream()
                .filter(cf -> cf.md5().equals(mostCommonRecordFileMd5EntryAndCount.getKey()))
                .findFirst()
                .orElse(null);
        // find most common md5 hash in all sidecar files
        final Map<String, Long> sidecarMd5Counts =
                sidecarFiles.stream().collect(Collectors.groupingBy(ChainFile::md5, Collectors.counting()));
        mostCommonSidecarFileMd5EntryAndCount = sidecarMd5Counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        if (mostCommonSidecarFileMd5EntryAndCount != null) {
            // find the first sidecar file with the most common md5 hash
            mostCommonSideCarFile = sidecarFiles.stream()
                    .filter(cf -> cf.md5().equals(mostCommonSidecarFileMd5EntryAndCount.getKey()))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Get the most common record file bytes, or null if there are no record files.
     */
    public byte[] getMostCommonRecordFileBytes(MainNetBucket mainNetBucket) {
        return mostCommonRecordFile == null ? null : mostCommonRecordFile.download(mainNetBucket);
    }

    public List<ChainFile> signatureFiles() {
        return signatureFiles;
    }

    /**
     * Get the most common sidecar file bytes, or null if there are no sidecar files.
     */
    public byte[] getMostCommonSidecarFileBytes(MainNetBucket mainNetBucket) {
        return mostCommonSideCarFile == null ? null : mostCommonSideCarFile.download(mainNetBucket);
    }

    @Override
    public String toString() {
        // check
        return """
               BlockInfo{
                   blockNum=$blockNum
                   blockTime=$blockTime
                   recordFiles[$recordFileCount]=$recordFiles
                       mostCommonRecordFileMd5=$mostCommonRecordFileMd5
                       filesMatching=$filesMatching of $recordFileCount = $filesMatchingPercent%
                       mostCommonRecordFile=$mostCommonRecordFile
                   signatureFiles[$signatureFilesSize]=$signatureFiles
                   sidecarFiles[$sidecarFilesSize]=$sidecarFiles
                       mostCommonSidecarFileMd5=$mostCommonSidecarFileMd5
                       filesMatching=$sidecarFilesMatching
               }"""
                .replace("$blockNum", String.valueOf(blockNum))
                .replace("$blockTime", String.valueOf(blockTime))
                .replace("$recordFileCount", String.valueOf(recordFiles.size()))
                .replace(
                        "$recordFiles",
                        recordFiles.stream().map(Objects::toString).collect(Collectors.joining(", ")))
                .replace("$mostCommonRecordFileMd5", mostCommonRecordFileMd5EntryAndCount.getKey())
                .replace(
                        "$filesMatching",
                        mostCommonRecordFileMd5EntryAndCount.getValue().toString())
                .replace(
                        "$filesMatchingPercent",
                        String.valueOf(((mostCommonRecordFileMd5EntryAndCount.getValue() / (double) recordFiles.size())
                                * 100)))
                .replace("$mostCommonRecordFile", mostCommonRecordFile.toString())
                .replace("$signatureFilesSize", String.valueOf(signatureFiles.size()))
                .replace(
                        "$signatureFiles",
                        signatureFiles.stream().map(Objects::toString).collect(Collectors.joining(", ")))
                .replace("$sidecarFilesSize", String.valueOf(sidecarFiles.size()))
                .replace(
                        "$sidecarFiles",
                        sidecarFiles.stream().map(Objects::toString).collect(Collectors.joining(", ")))
                .replace(
                        "$mostCommonSidecarFileMd5",
                        mostCommonSidecarFileMd5EntryAndCount == null
                                ? "none"
                                : mostCommonSidecarFileMd5EntryAndCount.getKey())
                .replace(
                        "$sidecarFilesMatching",
                        mostCommonSidecarFileMd5EntryAndCount == null
                                ? "none"
                                : mostCommonSidecarFileMd5EntryAndCount
                                        .getValue()
                                        .toString());
    }
}
