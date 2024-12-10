package com.hedera.block.tools.commands.record2blocks.mirrornode;

import static com.hedera.block.tools.commands.record2blocks.Main.DATA_DIR;
import static com.hedera.block.tools.commands.record2blocks.mirrornode.MirrorNodeConstants.RECORDS_CSV_FILE;
import static com.hedera.block.tools.commands.record2blocks.util.RecordFileDates.recordFileNameToBlockTimeLong;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * Read the record_file.csv.gz file from mirror node and count the number of lines in the file.
 */
@SuppressWarnings({"DuplicatedCode", "CallToPrintStackTrace"})
public class RecordFileCsvExtractBlockTimes {
    /**
     * The path to the block times file, this is a binary file of longs, each long is the number of nanoseconds for
     * that block after first block time. So first block = 0, second about 5 seconds later etc. The index is the block
     * number, so block 0 is first long, block 1 is second block and so on.
     */
    public static final Path BLOCK_TIMES_FILE = DATA_DIR.resolve("block_times.bin");
    /** the number of blocks in the record CSV file roughly */
    public static int NUMBER_OF_BLOCKS_ROUNDED_UP = 70_000_000;

    public static void main(String[] args) {
        // get the start time of the first block
        // create off heap array to store the block times
        final ByteBuffer blockTimesBytes = ByteBuffer.allocateDirect(NUMBER_OF_BLOCKS_ROUNDED_UP * Long.BYTES);
        final LongBuffer blockTimes = blockTimesBytes.asLongBuffer();
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
                // compute nanoseconds since the first block
                final long nanoseconds = recordFileNameToBlockTimeLong(recordStreamFileName);
                // write the block time to the off heap array
                blockTimes.put(blockNumber, nanoseconds);
                // print progress
                int currentBlockCount = blockCount.incrementAndGet();
                if (currentBlockCount % 100_000 == 0) {
                    System.out.printf("\rblock %,10d - %2.1f%% complete", currentBlockCount,
                            (currentBlockCount / 70_000_000f)*100);
                }
            });
            System.out.println("\nTotal blocks read = " + blockCount.get());
            // set limit to the number of blocks read
            final int totalBlockTimesBytes = blockCount.get() * Long.BYTES;
            blockTimesBytes.limit(totalBlockTimesBytes);
            blockTimes.limit(blockCount.get());
            // scan the block times to find any blocks missing times
            long totalBlocksWithoutTimes = 0;
            blockTimes.position(0);
            for (int i = 0; i < blockTimes.limit(); i++) {
                if (blockTimes.get(i) == 0) {
                    totalBlocksWithoutTimes++;
                    System.out.println("block["+i+"] is missing time - blockTimes[" + blockTimes.get(i) + "] = ");
                }
            }
            System.out.println("\ntotalBlocksWithoutTimes = " + totalBlocksWithoutTimes);
            // write the block times to a file
            try (final var out = Files.newByteChannel(BLOCK_TIMES_FILE, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                blockTimesBytes.position(0);
                long bytesWritten = out.write(blockTimesBytes);
                System.out.println("bytesWritten = " + bytesWritten);
                if (bytesWritten != totalBlockTimesBytes) {
                    System.out.println("ERROR: bytesWritten != totalBlockTimesBytes["+totalBlockTimesBytes+"]");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
