package com.hedera.block.tools.commands.record2blocks.model;

import static com.hedera.block.tools.commands.record2blocks.Main.MAX_NODE_ACCOUNT_ID;
import static com.hedera.block.tools.commands.record2blocks.Main.MIN_NODE_ACCOUNT_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BlockInfo {
    private final long blockNum;
    private final long blockTime;
    private final List<ChainFile> recordFiles = new ArrayList<>(MAX_NODE_ACCOUNT_ID - MIN_NODE_ACCOUNT_ID);
    private final List<ChainFile> signatureFiles = new ArrayList<>(MAX_NODE_ACCOUNT_ID - MIN_NODE_ACCOUNT_ID);
    private final List<ChainFile> sidecarFiles = new ArrayList<>(MAX_NODE_ACCOUNT_ID - MIN_NODE_ACCOUNT_ID);
    private Map.Entry<String, Long> mostCommonRecordFileMd5EntryAndCount;
    private ChainFile mostCommonRecordFile;
    private Map.Entry<String, Long> mostCommonSidecarFileMd5EntryAndCount;
    private ChainFile mostCommonSideCarFile;

    public BlockInfo(long blockNum, long blockTime) {
        this.blockNum = blockNum;
        this.blockTime = blockTime;
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
        final Map<String, Long> md5Counts = recordFiles.stream()
                .collect(Collectors.groupingBy(ChainFile::md5, Collectors.counting()));
        mostCommonRecordFileMd5EntryAndCount = md5Counts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        if (mostCommonRecordFileMd5EntryAndCount == null) {
            throw new IllegalStateException("No record files found");
        }
        // find the first record file with the most common md5 hash
        mostCommonRecordFile = recordFiles.stream()
                .filter(cf -> cf.md5().equals(mostCommonRecordFileMd5EntryAndCount.getKey()))
                .findFirst()
                .orElse(null);
        // find most common md5 hash in all sidecar files
        final Map<String, Long> sidecarMd5Counts = sidecarFiles.stream()
                .collect(Collectors.groupingBy(ChainFile::md5, Collectors.counting()));
        mostCommonSidecarFileMd5EntryAndCount = sidecarMd5Counts.entrySet()
                .stream()
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
    public byte[] getMostCommonRecordFileBytes() {
        return mostCommonRecordFile == null ? null : mostCommonRecordFile.download();
    }

    public List<ChainFile> signatureFiles() {
        return signatureFiles;
    }

    /**
     * Get the most common sidecar file bytes, or null if there are no sidecar files.
     */
    public byte[] getMostCommonSidecarFileBytes() {
        return mostCommonSideCarFile == null ? null : mostCommonSideCarFile.download();
    }

    @Override
    public String toString() {
        // check
        return "BlockInfo{\n" +
               "        blockNum= " + blockNum + "\n" +
               "        blockTime= " + blockTime + "\n" +
               "        recordFiles["+recordFiles.size()+"] = " + recordFiles.stream().map(Objects::toString).collect(Collectors.joining(", ")) + "\n" +
               "            mostCommonRecordFileMd5= \"" + mostCommonRecordFileMd5EntryAndCount.getKey() + "\"\n" +
               "            filesMatching= " + mostCommonRecordFileMd5EntryAndCount.getValue() +
                    " of "+recordFiles.size()+
                    " = "+((mostCommonRecordFileMd5EntryAndCount.getValue()/(double)recordFiles.size())*100)+"%\n" +
               "            mostCommonRecordFile= " + mostCommonRecordFile + "\n" +
               "        signatureFiles["+signatureFiles.size()+"] = " + signatureFiles.stream().map(Objects::toString).collect(Collectors.joining(", ")) + "\n" +
               "        sidecarFiles["+sidecarFiles.size()+"] = " + sidecarFiles.stream().map(Objects::toString).collect(Collectors.joining(", ")) + "\n" +
                "            mostCommonSidecarFileMd5= \"" + (mostCommonSidecarFileMd5EntryAndCount == null ? "none" : mostCommonSidecarFileMd5EntryAndCount.getKey()) + "\"\n" +
                "            filesMatching= " + (mostCommonSidecarFileMd5EntryAndCount == null ? "none" : mostCommonSidecarFileMd5EntryAndCount.getValue()) +
               "    }";
    }
}
