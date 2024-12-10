package com.hedera.block.tools.commands.record2blocks.mirrornode;

import static com.hedera.block.tools.commands.record2blocks.Main.DATA_DIR;

import java.nio.file.Path;

public class MirrorNodeConstants {
    public static final Path RECORDS_CSV_FILE = DATA_DIR.resolve("record_file.csv.gz");
}
