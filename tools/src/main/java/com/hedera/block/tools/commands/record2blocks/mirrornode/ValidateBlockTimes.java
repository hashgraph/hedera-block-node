package com.hedera.block.tools.commands.record2blocks.mirrornode;

import static com.hedera.block.tools.commands.record2blocks.mirrornode.MirrorNodeConstants.RECORDS_CSV_FILE;
import static com.hedera.block.tools.commands.record2blocks.util.RecordFileDates.blockTimeLongToRecordFilePrefix;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * Validate the block times in the block_times.bin file by comparing them to the record file names in CSV.
 */
@SuppressWarnings("DuplicatedCode")
public class ValidateBlockTimes {
    public static void main(String[] args) throws Exception{
        final BlockTimes blockTimes = new BlockTimes();
        // count the number of blocks to print progress
        final AtomicInteger blockCount = new AtomicInteger(0);
        // read the record file table CSV file
        try(var reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                new FileInputStream(RECORDS_CSV_FILE.toFile()))))) {
            // skip header
            reader.readLine();
            // read all lines
            reader.lines().parallel().forEach(line -> {
                final String[] parts = line.split(",");
                final String recordStreamFileName = parts[0];
                final int blockNumber = Integer.parseInt(parts[15]);
                // get the block offset from blockTimes
                final long blockTime = blockTimes.getBlockTime(blockNumber);
                // convert the block time to a string without 'Z' on the end
                String blockTimeString = blockTimeLongToRecordFilePrefix(blockTime);
                // check the file name starts with the block time
                if (!recordStreamFileName.startsWith(blockTimeString)) {
                    System.err.printf("Block %d has incorrect time %s should be %s%n", blockNumber,
                            recordStreamFileName, blockTimeString);
                }
                // print progress
                int currentBlockCount = blockCount.incrementAndGet();
                if (currentBlockCount % 100_000 == 0) {
                    System.out.printf("\rblock %,10d - %2.1f%% complete", currentBlockCount,
                            (currentBlockCount / 70_000_000f)*100);
                }
            });
        }
    }
}
