package com.hedera.block.tools;

import com.hedera.block.tools.commands.BlockInfo;
import com.hedera.block.tools.commands.ConvertToJson;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@SuppressWarnings("InstantiationOfUtilityClass")
@Command(name = "subcommands", mixinStandardHelpOptions = true, version = "BlockStreamTool 0.1",
        subcommands = {
            ConvertToJson.class, BlockInfo.class
        })
public class BlockStreamTool {

    /**
     * Main entry point for the app
     * @param args command line arguments
     */
    public static void main(String... args) {
        int exitCode = new CommandLine(new BlockStreamTool()).execute(args);
        System.exit(exitCode);
    }

}
