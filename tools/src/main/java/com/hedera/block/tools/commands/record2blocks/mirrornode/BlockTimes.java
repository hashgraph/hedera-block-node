package com.hedera.block.tools.commands.record2blocks.mirrornode;

import static com.hedera.block.tools.commands.record2blocks.mirrornode.RecordFileCsvExtractBlockTimes.BLOCK_TIMES_FILE;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * Read the block times from the block_times.bin file.
 */
public class BlockTimes {
    /** Mapped buffer on the block_times.bin file. */
    private final LongBuffer blockTimes;

    /**
     * Load and map the block_times.bin file into memory.
     */
    public BlockTimes() {
        try {
            // map file into bytebuffer
            final FileChannel fileChannel = FileChannel.open(BLOCK_TIMES_FILE, StandardOpenOption.READ);
            final ByteBuffer blockTimesBytes = fileChannel
                    .map(FileChannel.MapMode.READ_ONLY, 0, Files.size(BLOCK_TIMES_FILE));
            fileChannel.close();
            // wrap the ByteBuffer as a LongBuffer so we can easily read longs
            blockTimes = blockTimesBytes.asLongBuffer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the block time for the given block number.
     *
     * @param blockNumber the block number
     * @return the block time in milliseconds
     */
    public long getBlockTime(int blockNumber) {
        return blockTimes.get(blockNumber);
    }

    /**
     * Get the maximum block number in the block_times.bin file.
     *
     * @return the maximum block number
     */
    public long getMaxBlockNumber() {
        return blockTimes.capacity();
    }
}
