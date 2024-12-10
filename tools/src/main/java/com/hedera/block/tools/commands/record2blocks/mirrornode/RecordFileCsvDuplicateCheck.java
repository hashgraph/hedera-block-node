package com.hedera.block.tools.commands.record2blocks.mirrornode;

import static com.hedera.block.tools.commands.record2blocks.Main.DATA_DIR;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.zip.GZIPInputStream;

/**
 * Read the record_file.csv.gz file from mirror node and count the number of lines in the file.
 */
public class RecordFileCsvDuplicateCheck {
    public static void main(String[] args) {
        Path recordFile = DATA_DIR.resolve("record_file.csv.gz");
        AtomicIntegerArray blockNodeCounts = new AtomicIntegerArray(70_000_000);
        try(var reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(recordFile.toFile()))))) {
            // skip header
            reader.readLine();
            // read all lines
            reader.lines().parallel().forEach(line -> {
                String[] parts = line.split(",");
                int index = Integer.parseInt(parts[15]);
                blockNodeCounts.incrementAndGet(index);
            });
            long totalBlocksWithDuplicates = 0;
            for (int i = 0; i < blockNodeCounts.length(); i++) {
                if (blockNodeCounts.get(i) > 1) {
                    totalBlocksWithDuplicates++;
                    System.out.println("blockNodeCounts[" + i + "] = " + blockNodeCounts.get(i));
                }
            }
            System.out.println("\ntotalBlocksWithDuplicates = " + totalBlocksWithDuplicates);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
