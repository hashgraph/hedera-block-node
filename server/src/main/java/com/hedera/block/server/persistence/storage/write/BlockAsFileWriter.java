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

package com.hedera.block.server.persistence.storage.write;

import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BlockAsFileWriter implements BlockWriter<Block> {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final Path blockNodeRootPath;
    private final MetricsService metricsService;
    private final boolean compressBlocks;
    private final String compressionAlgorithm;

    private final List<BlockItem> currentBlockItems = new ArrayList<>();
    private long currentBlockNumber;

    public BlockAsFileWriter(
            @NonNull final PersistenceStorageConfig config,
            @NonNull final MetricsService metricsService) {
        this.blockNodeRootPath = Path.of(config.rootPath());
        this.metricsService = metricsService;
        this.compressBlocks = config.enableCompression();
        this.compressionAlgorithm = config.compressionAlgorithm();
    }

    @Override
    public Optional<Block> write(@NonNull Block block) throws IOException {

        writeBlockToFile(block);

        return Optional.of(block);
    }

    private long getBlockNumber(Block block) {
        return block.items().getFirst().blockHeader().number();
    }

    private void writeBlockToFile(Block block) throws IOException {

        currentBlockNumber = getBlockNumber(block);

        // get initial size
        byte[] blockBytes = Block.PROTOBUF.toBytes(block).toByteArray();
        long initialSize = blockBytes.length;

        // create file name
        String fileName = getBlockFilename(currentBlockNumber);
        Path blockFilePath = blockNodeRootPath.resolve(fileName);

        // Start timing
        // TODO remove once we do a final impl.
        long startTime = System.nanoTime();

        // write block to file
        try (OutputStream os = Files.newOutputStream(blockFilePath)) {
            if (compressBlocks) {
                if (compressionAlgorithm.equals("zip")) {
                    try (ZipOutputStream zos = new ZipOutputStream(os)) {
                        // zos.setLevel(Deflater.NO_COMPRESSION);

                        // Create a new zip entry for the block
                        ZipEntry entry = new ZipEntry(fileName);

                        zos.putNextEntry(entry);

                        // Write the block data to the zip entry
                        Block.PROTOBUF.toBytes(block).writeTo(zos);

                        // Close the current zip entry
                        zos.closeEntry();
                    }
                } else { // if compressed default is gzip
                    try (GZIPOutputStream gos = new GZIPOutputStream(os)) {
                        Block.PROTOBUF.toBytes(block).writeTo(gos);
                    }
                }
            } else {
                Block.PROTOBUF.toBytes(block).writeTo(os);
            }
        }

        // End timing
        // TODO remove once we do a final impl.
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);

        // Get compressed file size
        // TODO remove once we do a final impl.
        BasicFileAttributes attrs = Files.readAttributes(blockFilePath, BasicFileAttributes.class);
        long compressedSize = attrs.size();

        // log metrics
        // TODO remove once we do a final impl.
        LOGGER.log(
                System.Logger.Level.INFO,
                "Block {0} written to file {1} in {2} NS. Total Items: {5} Initial size: {3} bytes"
                        + " Compressed size: {4} bytes",
                currentBlockNumber,
                blockFilePath,
                duration,
                initialSize,
                compressedSize,
                block.items().size());
    }

    /**
     * Convert a long to a 36-character string, padded with leading zeros.
     *
     * @param value the long to convert
     * @return the 36-character string padded with leading zeros
     */
    @NonNull
    private static String longToFileName(final long value) {
        // Convert the signed long to an unsigned long using BigInteger for correct representation
        BigInteger unsignedValue =
                BigInteger.valueOf(value & Long.MAX_VALUE)
                        .add(BigInteger.valueOf(Long.MIN_VALUE & value));

        // Format the unsignedValue as a 36-character string, padded with leading zeros to ensure we
        // have enough digits
        // for an unsigned long. However, to allow for future expansion, we use 36 characters as
        // that's what UUID uses.
        return String.format("%036d", unsignedValue);
    }

    private String getBlockFilename(long blockNumber) {
        if (compressBlocks) {
            if (compressionAlgorithm.equals("zip")) {
                return longToFileName(blockNumber) + ".blk.zip";
            } else {
                // if compressed default is gzip
                return longToFileName(blockNumber) + ".blk.gz";
            }
        } else {
            return longToFileName(blockNumber) + ".blk";
        }
    }
}
