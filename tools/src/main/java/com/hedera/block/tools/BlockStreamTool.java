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

package com.hedera.block.tools;

import com.hedera.block.tools.commands.BlockInfo;
import com.hedera.block.tools.commands.ConvertToJson;
import com.hedera.block.tools.commands.record2blocks.Record2BlockCommand;
import com.hedera.block.tools.commands.record2blocks.mirrornode.RecordFileCsvExtractBlockTimes;
import com.hedera.block.tools.commands.record2blocks.mirrornode.ValidateBlockTimes;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Command line tool for working with Hedera block stream files
 */
@SuppressWarnings("InstantiationOfUtilityClass")
@Command(
        name = "subcommands",
        mixinStandardHelpOptions = true,
        version = "BlockStreamTool 0.1",
        subcommands = {ConvertToJson.class, BlockInfo.class, Record2BlockCommand.class,
                RecordFileCsvExtractBlockTimes.class, ValidateBlockTimes.class})
public final class BlockStreamTool {

    /**
     * Empty Default constructor to remove JavaDoc warning
     */
    public BlockStreamTool() {}

    /**
     * Main entry point for the app
     * @param args command line arguments
     */
    public static void main(String... args) {
        int exitCode = new CommandLine(new BlockStreamTool()).execute(args);
        System.exit(exitCode);
    }
}
