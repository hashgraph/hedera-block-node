package com.hedera.block.tools;

import com.hedera.block.tools.commands.BlockInfo;
import com.hedera.block.tools.commands.ConvertToJson;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Command line tool for working with Hedera block stream files
 */
@SuppressWarnings("InstantiationOfUtilityClass")
@Command(name = "subcommands", mixinStandardHelpOptions = true, version = "BlockStreamTool 0.1",
        subcommands = {
            ConvertToJson.class, BlockInfo.class
        })
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
