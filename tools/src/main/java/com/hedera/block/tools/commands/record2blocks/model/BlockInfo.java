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

import com.hedera.block.tools.commands.record2blocks.model.ChainFile.Kind;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import picocli.CommandLine.Help.Ansi;

/**
 * BlockInfo represents a Hedera block with its associated record files, sidecar files and signature files.
 */
@SuppressWarnings("unused")
public record BlockInfo(
    long blockNum,
    long blockTime,
    List<ChainFile> recordFiles,
    ChainFileAndCount mostCommonRecordFile,
    SortedMap<Integer, NumberedSidecarFile> sidecarFiles,
    List<ChainFile> signatureFiles
){

    public BlockInfo(long blockNum, long blockTime, List<ChainFile> allBlockFiles) {
        this(
                blockNum,
                blockTime,
                allBlockFiles.stream().filter(cf -> cf.kind() == Kind.RECORD).collect(Collectors.toList()),
                mostCommonRecordFileMd5EntryAndCount(allBlockFiles),
                collectSidecarFiles(allBlockFiles),
                allBlockFiles.stream().filter(cf -> cf.kind() == Kind.SIGNATURE).collect(Collectors.toList())
        );
    }

    private static ChainFileAndCount mostCommonRecordFileMd5EntryAndCount(List<ChainFile> allBlockFiles){
        final Map<String, Long> md5Counts =
                allBlockFiles.stream()
                        .filter(cf -> cf.kind() == Kind.RECORD)
                        .collect(Collectors.groupingBy(ChainFile::md5, Collectors.counting()));
        final var maxCountentry = md5Counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (maxCountentry == null) {
            throw new IllegalStateException("No record files found");
        }
        final ChainFile maxCountRecordFile = allBlockFiles.stream()
                .filter(cf -> cf.md5().equals(maxCountentry.getKey()))
                .findFirst()
                .orElse(null);
        return new ChainFileAndCount(maxCountRecordFile, maxCountentry.getValue().intValue());
    }

    private static SortedMap<Integer, NumberedSidecarFile> collectSidecarFiles(List<ChainFile> allBlockFiles){
        // group sidecar files by sidecar index
        final Map<Integer, List<ChainFile>> sidecarFiles = allBlockFiles.stream()
                .filter(cf -> cf.kind() == Kind.SIDECAR)
                .collect(Collectors.groupingBy(ChainFile::sidecarIndex));
        final TreeMap<Integer, NumberedSidecarFile> sortedSidecarFiles = new TreeMap<Integer, NumberedSidecarFile>();
        sidecarFiles.forEach((sidecarIndex, sidecarFileList) -> {
            sortedSidecarFiles.put(sidecarIndex, new NumberedSidecarFile(sidecarFileList));
        });
        return sortedSidecarFiles;
    }

    /** Template used for rendering to string. */
    private static final String TEMPLATE = Ansi.AUTO.string(
            "@|bold,yellow BlockInfo{|@  @|yellow blockNum=|@$blockNum, @|yellow blockTime=|@$blockTime "+
            "@|bold,yellow recordFiles|@ @|yellow total=|@$recordFileCount @|yellow " +
            "matching=|@$recordFilesMatching @|cyan -> $recordFilePercent%|@ " +
            "@|bold,yellow sidecarFiles=|@  $sidecarFiles" +
            "@|bold,yellow signatureFiles|@ @|yellow total=|@$signatureFilesSize " +
            "@|bold,yellow }|@");

    @Override
    public String toString() {
        // check
        return TEMPLATE
                .replace("$blockNum", String.valueOf(blockNum))
                .replace("$blockTime", String.valueOf(blockTime))
                .replace("$recordFileCount", String.valueOf(recordFiles.size()))
                .replace("$recordFilesMatching",String.valueOf(mostCommonRecordFile.count()))
                .replace("$recordFilePercent",
                        String.valueOf(((mostCommonRecordFile.count() / (double) recordFiles.size()) * 100)))
                .replace("$mostCommonRecordFile", mostCommonRecordFile.toString())
                .replace(
                        "$sidecarFiles",
                        sidecarFiles.isEmpty() ? "none" :
                        sidecarFiles.values().stream().map(NumberedSidecarFile::toString).collect(Collectors.joining(", ")))
                .replace("$signatureFilesSize", String.valueOf(signatureFiles.size()));
    }
}
