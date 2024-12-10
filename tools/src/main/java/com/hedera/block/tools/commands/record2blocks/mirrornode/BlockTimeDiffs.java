package com.hedera.block.tools.commands.record2blocks.mirrornode;

import static com.hedera.block.tools.commands.record2blocks.Main.DATA_DIR;

import com.hedera.pbj.runtime.io.stream.WritableStreamingData;
import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This was an experiment to compress BlockTimes data into a file of block time differences. It takes data
 */
public class BlockTimeDiffs {
    public static final Path BLOCK_TIMES_DIFFS_FILE = DATA_DIR.resolve("block_time_diffs.txt");
    public static void main(String[] args) {
        BlockTimes blockTimes = new BlockTimes();
//        for(int i = 1; i < blockTimes.getMaxBlockNumber(); i++) {
        try(OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(BLOCK_TIMES_DIFFS_FILE)))) {
            long lastBlockTime = 0;
            for(int i = 1; i < blockTimes.getMaxBlockNumber(); i++) {
                if (i % 100_000 == 0) {
                    System.out.print("\rWriting block "+i);
                }
                final long blockTime = blockTimes.getBlockTime(i);
                final long diffLong = (blockTime - lastBlockTime);
                out.write(Long.toString(diffLong));
                out.write('\n');
                lastBlockTime = blockTime;
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main2(String[] args) {
        BlockTimes blockTimes = new BlockTimes();
//        for(int i = 1; i < blockTimes.getMaxBlockNumber(); i++) {
        try (WritableStreamingData dout = new WritableStreamingData(
                new BufferedOutputStream(Files.newOutputStream(BLOCK_TIMES_DIFFS_FILE)))) {
            long lastBlockTime = 0;
            for (int i = 1; i < blockTimes.getMaxBlockNumber(); i++) {
                if (i % 100_000 == 0) {
                    System.out.print("\rWriting block " + i);
                }
                final long blockTime = blockTimes.getBlockTime(i);
                final long diffLong = (blockTime - lastBlockTime);
                dout.writeVarLong(diffLong, false);
                lastBlockTime = blockTime;
            }
            dout.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    public static int longToUnsignedInt(long value) {
        if (value < 0 || value > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("Value out of range for unsigned int: " + value);
        }
        return (int) (value & 0xFFFFFFFFL);
    }
}
