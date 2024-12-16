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

package com.hedera.block.tools.commands.record2blocks;

import static com.hedera.block.tools.commands.record2blocks.util.BlockWriter.writeBlock;
import static com.hedera.block.tools.commands.record2blocks.util.RecordFileDates.blockTimeLongToInstant;

import com.hedera.block.tools.commands.record2blocks.gcp.MainNetBucket;
import com.hedera.block.tools.commands.record2blocks.model.BlockInfo;
import com.hedera.block.tools.commands.record2blocks.model.BlockTimes;
import com.hedera.block.tools.commands.record2blocks.model.ChainFile;
import com.hedera.block.tools.commands.record2blocks.model.SignatureFile;
import com.hedera.block.tools.commands.record2blocks.util.BlockWriter.BlockPath;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.block.stream.BlockItem.ItemOneOfType;
import com.hedera.hapi.block.stream.RecordFileItem;
import com.hedera.hapi.node.base.BlockHashAlgorithm;
import com.hedera.hapi.node.base.Timestamp;
import com.hedera.pbj.runtime.OneOf;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.hedera.pbj.runtime.io.stream.WritableStreamingData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Command line command that converts a record stream to blocks
 */
@SuppressWarnings("FieldCanBeLocal")
@Command(name = "record2block", description = "Converts a record stream files into blocks")
public class Record2BlockCommand implements Runnable {

    @Option(
            names = {"-s", "--start-block"},
            description = "The block to start converting from")
    private int startBlock = 0;

    @Option(
            names = {"-e", "--end-block"},
            description = "The block to end converting at")
    private int endBlock = 3001;

    @Option(
            names = {"-j", "--json"},
            description = "also output blocks as json")
    private boolean jsonEnabled = false;

    @Option(
            names = {"-c", "--cache-enabled"},
            description = "Use local cache for downloaded content")
    private boolean cacheEnabled = false;

    @Option(
            names = {"--min-node-account-id"},
            description = "the account id of the first node in the network")
    private int minNodeAccountId = 3;

    @Option(
            names = {"--max-node-account-id"},
            description = "the account id of the last node in the network")
    private int maxNodeAccountId = 34;

    @Option(
            names = {"-d", "--data-dir"},
            description = "the data directory for output and temporary files")
    private Path dataDir = Path.of("data");

    /** The path to the block times file. */
    @Option(
            names = {"--block-times"},
            description = "Path to the block times \".bin\" file.")
    private Path blockTimesFile = Path.of("data/block_times.bin");

    /**
     * Path to the output blocks directory
     */
    private Path blocksDir;

    /**
     * Path to the output json blocks directory
     */
    private Path blocksJsonDir;

    /**
     * Empty Default constructor to remove JavaDoc warning
     */
    public Record2BlockCommand() {}

    /**
     * Main method to run the command
     */
    @Override
    public void run() {
        try {
            blocksDir = dataDir.resolve("blocks");
            blocksJsonDir = dataDir.resolve("blocks-json");
            // enable cache, disable if doing large batches
            final MainNetBucket mainNetBucket =
                    new MainNetBucket(cacheEnabled, dataDir.resolve("gcp-cache"), minNodeAccountId, maxNodeAccountId);
            // create blocks dir
            Files.createDirectories(blocksDir);
            if (jsonEnabled) {
                Files.createDirectories(blocksJsonDir);
            }
            // check start block is less than end block
            if (startBlock > endBlock) {
                throw new IllegalArgumentException("Start block must be less than end block");
            }
            // check blockTimesFile exists
            if (!Files.exists(blockTimesFile)) {
                throw new IllegalArgumentException("Block times file does not exist: " + blockTimesFile);
            }
            // map the block_times.bin file
            final BlockTimes blockTimes = new BlockTimes(blockTimesFile);
            // iterate over the blocks
            Instant currentHour = null;
            List<ChainFile> currentHoursFiles = null;
            for (int blockNumber = startBlock; blockNumber <= endBlock; blockNumber++) {
                // get the time of the record file for this block, from converted mirror node data
                final long blockTime = blockTimes.getBlockTime(blockNumber);
                final Instant blockTimeInstant = blockTimeLongToInstant(blockTime);
                System.out.println(Ansi.AUTO.string("@|bold,green,underline Processing block|@ " + blockNumber
                        + " @|green at blockTime|@ " + blockTimeInstant));
                // round instant to nearest hour
                Instant blockTimeHour = blockTimeInstant.truncatedTo(ChronoUnit.HOURS);
                // check if we are the same hour as last block, if not load the new hour
                if (currentHour == null || !currentHour.equals(blockTimeHour)) {
                    currentHour = blockTimeHour;
                    System.out.print(Ansi.AUTO.string("@|bold,yellow    Listing files from GCP ...|@"));
                    currentHoursFiles = mainNetBucket.listHour(blockTime);
                    System.out.println(Ansi.AUTO.string(
                            "\r@|bold,yellow    Listed " + currentHoursFiles.size() + " files from GCP|@"));
                }
                // create block info
                BlockInfo blockInfo = new BlockInfo(
                        blockNumber,
                        blockTime,
                        currentHoursFiles.stream()
                                .filter(cf -> cf.blockTime() == blockTime)
                                .toList());
                // print block info
                System.out.println("   " + blockInfo);
                // now we need to download the most common record file
                // we will use the GCP bucket to download the file
                byte[] recordFileBytes =
                        blockInfo.mostCommonRecordFile().chainFile().download(mainNetBucket);

                // download and parse all signature files
                SignatureFile[] signatureFileBytes = blockInfo.signatureFiles().stream()
                        .parallel()
                        .map(chainFile -> chainFile.download(mainNetBucket))
                        .map(SignatureFile::parse)
                        .toArray(SignatureFile[]::new);
                for (SignatureFile signatureFile : signatureFileBytes) {
                    // System.out.println("        signatureFile = " + signatureFile);
                }

                // download most common sidecar file
                List<Bytes> sideCars = new ArrayList<>();
                //                byte[] sidecarFileBytes = blockInfo.getMostCommonSidecarFileBytes(mainNetBucket);

                // build new Block File
                final RecordFileItem recordFileItem = new RecordFileItem(
                        blockInfo.blockNum(),
                        new Timestamp(blockTimeInstant.getEpochSecond(), blockTimeInstant.getNano()),
                        Bytes.wrap(recordFileBytes),
                        sideCars,
                        BlockHashAlgorithm.SHA2_384,
                        Arrays.stream(signatureFileBytes)
                                .map(sigFile -> Bytes.wrap(sigFile.signature()))
                                .toList());
                final Block block = new Block(Collections.singletonList(
                        new BlockItem(new OneOf<>(ItemOneOfType.RECORD_FILE, recordFileItem))));
                // write block to disk
                final BlockPath blockPath = writeBlock(blocksDir, block);
                System.out.println(Ansi.AUTO.string("@|bold,yellow    Wrote block to|@ " + blockPath.dirPath()
                        + "/" + blockPath.zipFileName() + "@|bold,cyan :|@"
                        + blockPath.blockFileName()));
                // write as json for now as well
                if (jsonEnabled) {
                    final Path blockJsonPath = blocksJsonDir.resolve(blockPath.blockNumStr() + ".blk.json");
                    Files.createDirectories(blockJsonPath.getParent());
                    try (WritableStreamingData out = new WritableStreamingData(Files.newOutputStream(
                            blockJsonPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
                        Block.JSON.write(block, out);
                    }
                    System.out.println(Ansi.AUTO.string("@|bold,yellow    Wrote block json to|@ " + blockJsonPath));
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
