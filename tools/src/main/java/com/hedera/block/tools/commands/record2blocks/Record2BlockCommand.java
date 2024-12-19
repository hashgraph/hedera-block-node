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

import static com.hedera.block.tools.commands.record2blocks.mirrornode.FetchBlockQuery.getPreviousHashForBlock;
import static com.hedera.block.tools.commands.record2blocks.util.BlockWriter.writeBlock;
import static com.hedera.block.tools.commands.record2blocks.util.RecordFileDates.blockTimeLongToInstant;

import com.hedera.block.tools.commands.record2blocks.gcp.MainNetBucket;
import com.hedera.block.tools.commands.record2blocks.model.BlockInfo;
import com.hedera.block.tools.commands.record2blocks.model.BlockTimes;
import com.hedera.block.tools.commands.record2blocks.model.ChainFile;
import com.hedera.block.tools.commands.record2blocks.model.ParsedSignatureFile;
import com.hedera.block.tools.commands.record2blocks.model.RecordFileVersionInfo;
import com.hedera.block.tools.commands.record2blocks.util.BlockWriter.BlockPath;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.block.stream.BlockItem.ItemOneOfType;
import com.hedera.hapi.block.stream.RecordFileItem;
import com.hedera.hapi.block.stream.RecordFileSignature;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.hapi.node.base.BlockHashAlgorithm;
import com.hedera.hapi.node.base.SemanticVersion;
import com.hedera.hapi.node.base.Timestamp;
import com.hedera.hapi.streams.SidecarFile;
import com.hedera.pbj.runtime.OneOf;
import com.hedera.pbj.runtime.ParseException;
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
import java.util.HexFormat;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Command line command that converts a record stream to blocks
 * <p>
 *     Example block ranges for testing:
 *     <ul>
 *         <li><code>-s 0 -e 10</code> - Record File v2</li>
 *         <li><code>-s 12877843 -e 12877853</code> - Record File v5</li>
 *         <li><code>-s 72756872 -e 72756882</code> - Record File v6 with sidecars</li>
 *     </ul>
 *     Record files start at V2 at block 0 then change to V5 at block 12370838 and V6 at block 38210031
 * </p>
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
            // get previous block hash
            Bytes previousBlockHash;
            if (startBlock == 0) {
                previousBlockHash = Bytes.wrap(new byte[48]); // empty hash for first block
            } else {
                // get previous block hash from mirror node
                previousBlockHash = getPreviousHashForBlock(startBlock);
            }
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
                byte[] recordFileBytes =
                        blockInfo.mostCommonRecordFile().chainFile().download(mainNetBucket);

                // parse version information out of record file
                final RecordFileVersionInfo recordFileVersionInfo = RecordFileVersionInfo.parse(recordFileBytes);

                // download and parse all signature files
                ParsedSignatureFile[] signatureFileBytes = blockInfo.signatureFiles().stream()
                        .parallel()
                        .map(cf -> ParsedSignatureFile.downloadAndParse(cf, mainNetBucket))
                        .toArray(ParsedSignatureFile[]::new);
                // convert signature files to list of RecordFileSignatures
                final List<RecordFileSignature> recordFileSignatures = Arrays.stream(signatureFileBytes)
                        .map(sigFile -> new RecordFileSignature(Bytes.wrap(sigFile.signature()), sigFile.nodeId()))
                        .toList();

                // download most common sidecar file
                List<SidecarFile> sideCars = blockInfo.sidecarFiles().values().stream()
                        .map(sidecarFile -> {
                            byte[] sidecarFileBytes = sidecarFile.mostCommonSidecarFile().chainFile().download(mainNetBucket);
                            try {
                                return SidecarFile.PROTOBUF.parse(Bytes.wrap(sidecarFileBytes));
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                        }).toList();

                // build new Block File
                final BlockHeader blockHeader = new BlockHeader(
                        recordFileVersionInfo.hapiProtoVersion(),
                        recordFileVersionInfo.hapiProtoVersion(),
                        blockNumber,
                        previousBlockHash,
                        new Timestamp(blockTimeInstant.getEpochSecond(), blockTimeInstant.getNano()),
                        BlockHashAlgorithm.SHA2_384);
                final RecordFileItem recordFileItem = new RecordFileItem(
                        new Timestamp(blockTimeInstant.getEpochSecond(), blockTimeInstant.getNano()),
                        Bytes.wrap(recordFileBytes),
                        sideCars,recordFileSignatures
                        );
                final Block block = new Block(List.of(
                        new BlockItem(new OneOf<>(ItemOneOfType.BLOCK_HEADER, blockHeader)),
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
                // update previous block hash
                previousBlockHash = recordFileVersionInfo.blockHash();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
