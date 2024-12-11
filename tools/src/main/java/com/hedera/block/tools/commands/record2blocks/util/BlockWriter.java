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

package com.hedera.block.tools.commands.record2blocks.util;

import com.github.luben.zstd.ZstdOutputStream;
import com.hedera.hapi.block.stream.Block;
import com.hedera.pbj.runtime.io.stream.WritableStreamingData;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

/**
 * Utility class for creating paths to block files, in block node format.
 */
@SuppressWarnings("DataFlowIssue")
public class BlockWriter {
    public static final NumberFormat BLOCK_NUMBER_FORMAT = new DecimalFormat("0000000000000000000");
    private static final String COMPRESSED_BLOCK_FILE_EXTENSION = ".blk.zstd";
    private static final int DEFAULT_DIGITS_PER_DIR = 3;
    private static final int DEFAULT_DIGEST_PER_ZIP = 3;
    private static final int DEFAULT_DIGEST_PER_ZIP_FILE_NAME = 1;

    public static void writeBlock(final Path baseDirectory, final Block block) throws IOException {
        // get block number from block header
        final var firstBlockItem = block.items().getFirst();
        final long blockNumber =
                switch (firstBlockItem.item().kind()) {
                    case BLOCK_HEADER -> firstBlockItem.blockHeader().number();
                    case RECORD_FILE -> firstBlockItem.recordFile().number();
                    default -> throw new IllegalArgumentException(
                            "Block first item is not a block header or record file");
                };
        // convert block number to string
        final String blockNumberStr = BLOCK_NUMBER_FORMAT.format(blockNumber);
        // split string into digits for zip and for directories
        final int offsetToZip = blockNumberStr.length() - DEFAULT_DIGEST_PER_ZIP_FILE_NAME - DEFAULT_DIGITS_PER_DIR;
        final String directoryDigits = blockNumberStr.substring(0, offsetToZip);
        final String zipFileNameDigits =
                blockNumberStr.substring(offsetToZip, offsetToZip + DEFAULT_DIGEST_PER_ZIP_FILE_NAME);
        final String zipContentsDigits = blockNumberStr.substring(blockNumberStr.length() - DEFAULT_DIGEST_PER_ZIP);
        System.out.println("blockNumberStr = " + blockNumberStr);
        System.out.println("                 " + directoryDigits + " " + zipFileNameDigits + " " + zipContentsDigits);
        System.out.println("                 directoryDigits zipFileNameDigits zipContentsDigits");
        // start building path to zip file
        Path dirPath = baseDirectory;
        for (int i = 0; i < directoryDigits.length(); i += DEFAULT_DIGITS_PER_DIR) {
            final String dirName =
                    directoryDigits.substring(i, Math.min(i + DEFAULT_DIGITS_PER_DIR, directoryDigits.length()));
            dirPath = dirPath.resolve(dirName);
        }
        // create directories
        Files.createDirectories(dirPath);
        // create zip file name
        final String zipFileName = zipFileNameDigits + "000.zip";
        final Path zipPath = dirPath.resolve(zipFileName);
        // append block to zip file, creating zip file if it doesn't exist
        try (FileSystem fs = FileSystems.newFileSystem(
                URI.create("jar:" + zipPath.toUri()), Map.of("create", "true", "compressionMethod", "STORED"))) {
            final String fileName = blockNumberStr + COMPRESSED_BLOCK_FILE_EXTENSION;
            Path blockPathInZip = fs.getPath(fileName);
            System.out.println("blockPathInZip = " + blockPathInZip);
            try (WritableStreamingData out = new WritableStreamingData(new ZstdOutputStream(
                    Files.newOutputStream(blockPathInZip, StandardOpenOption.CREATE, StandardOpenOption.WRITE)))) {
                Block.PROTOBUF.write(block, out);
            }
        }
    }

    /**
     * Record for block path components
     *
     * @param dirPath The directory path for the directory that contains the zip file
     * @param zipFileName The name of the zip file
     * @param blockFileName The name of the block file in the zip file
     */
    public record BlockPath(Path dirPath, String zipFileName, String blockFileName) {}

    /**
     * Compute the path to a block file
     *
     * @param baseDirectory The base directory for the block files
     * @param blockNumber The block number
     * @return The path to the block file
     */
    public static BlockPath computeBlockPath(final Path baseDirectory, long blockNumber) {
        // convert block number to string
        final String blockNumberStr = BLOCK_NUMBER_FORMAT.format(blockNumber);
        // split string into digits for zip and for directories
        final int offsetToZip = blockNumberStr.length() - DEFAULT_DIGEST_PER_ZIP_FILE_NAME - DEFAULT_DIGITS_PER_DIR;
        final String directoryDigits = blockNumberStr.substring(0, offsetToZip);
        final String zipFileNameDigits =
                blockNumberStr.substring(offsetToZip, offsetToZip + DEFAULT_DIGEST_PER_ZIP_FILE_NAME);
        // start building path to zip file
        Path dirPath = baseDirectory;
        for (int i = 0; i < directoryDigits.length(); i += DEFAULT_DIGITS_PER_DIR) {
            final String dirName =
                    directoryDigits.substring(i, Math.min(i + DEFAULT_DIGITS_PER_DIR, directoryDigits.length()));
            dirPath = dirPath.resolve(dirName);
        }
        // create zip file name
        final String zipFileName = zipFileNameDigits + "000s.zip";
        final String fileName = blockNumberStr + COMPRESSED_BLOCK_FILE_EXTENSION;
        return new BlockPath(dirPath, zipFileName, fileName);
    }

    /**
     * Simple main method to test the block path computation
     */
    public static void main(String[] args) {
        for (long blockNumber = 0; blockNumber < 3002; blockNumber++) {
            final var blockPath = computeBlockPath(Path.of("data"), blockNumber);
            System.out.println("blockPath = " + blockPath);
        }
    }
}
