package com.hedera.block.tools.commands.record2blocks.mirrornode;

import static com.hedera.block.tools.commands.record2blocks.Main.DATA_DIR;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * Read the record_file.csv.gz file from mirror node and count the number of lines in the file.
 */
public class RecordFileCsvLineCount {
    public static void main(String[] args) {
        Path recordFile = DATA_DIR.resolve("record_file.csv.gz");

        try(var reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(recordFile.toFile()))))) {
            // skip header
            reader.readLine();
            // read all lines
            long totalLines = reader.lines().count();
            System.out.println("totalLines = " + totalLines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
