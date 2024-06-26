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

package com.hedera.block.server.persistence.storage;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import io.helidon.config.Config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.hedera.block.server.Constants.BLOCKNODE_STORAGE_ROOT_PATH_KEY;

/**
 * The FileSystemBlockStorage class implements the BlockStorage interface to store blocks to the filesystem.
 */
public class FileSystemBlockStorage implements BlockStorage<BlockStreamServiceGrpcProto.Block> {

    public static final String BLOCK_FILE_EXTENSION = ".blk";

    private final Path blockNodeRootPath;
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    /**
     * Constructs a FileSystemBlockStorage object.
     *
     * @param key the key to use to retrieve the block node root path from the configuration
     * @param config the configuration
     * @throws IOException if an I/O error occurs while initializing the block node root directory
     */
    public FileSystemBlockStorage(final String key, final Config config) throws IOException {

        LOGGER.log(System.Logger.Level.INFO, "Initializing FileSystemBlockStorage");
        LOGGER.log(System.Logger.Level.INFO, config.toString());

        blockNodeRootPath = Path.of(config
                .get(key)
                .asString()
                .get());

        LOGGER.log(System.Logger.Level.INFO, "Block Node Root Path: " + blockNodeRootPath);

        if (!blockNodeRootPath.isAbsolute()) {
            throw new IllegalArgumentException(BLOCKNODE_STORAGE_ROOT_PATH_KEY+ " must be an absolute path");
        }

        // Initialize the block node root directory if it does not exist
        if (Files.notExists(blockNodeRootPath)) {
            Files.createDirectory(blockNodeRootPath);
            LOGGER.log(System.Logger.Level.INFO, "Created block node root directory: " + blockNodeRootPath);
        } else {
            LOGGER.log(System.Logger.Level.INFO, "Using existing block node root directory: " + blockNodeRootPath);
        }
    }

    /**
     * Writes a block to the filesystem.
     *
     * @param block the block to write
     * @return the id of the block
     */
    @Override
    public Optional<Long> write(final BlockStreamServiceGrpcProto.Block block) {
        Long id = block.getId();
        final String fullPath = resolvePath(id);

        try (FileOutputStream fos = new FileOutputStream(fullPath)) {
            block.writeTo(fos);
            LOGGER.log(System.Logger.Level.DEBUG, "Successfully wrote the block file: " + fullPath);

            return Optional.of(id);
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Error writing the protobuf to a file", e);
            return Optional.empty();
        }
    }

    /**
     * Reads a block from the filesystem.
     *
     * @param id the id of the block to read
     * @return the block
     */
    @Override
    public Optional<BlockStreamServiceGrpcProto.Block> read(final Long id) {
        return read(resolvePath(id));
    }

    private Optional<BlockStreamServiceGrpcProto.Block> read(final String filePath) {

        try (FileInputStream fis = new FileInputStream(filePath)) {
            return Optional.of(BlockStreamServiceGrpcProto.Block.parseFrom(fis));
        } catch (FileNotFoundException io) {
            LOGGER.log(System.Logger.Level.ERROR, "Error reading file: " + filePath, io);
            return Optional.empty();
        } catch (IOException io) {
            throw new RuntimeException("Error reading file: " + filePath, io);
        }
    }

    private String resolvePath(final Long id) {

        String fileName = id + BLOCK_FILE_EXTENSION;
        Path fullPath = blockNodeRootPath.resolve(fileName);
        LOGGER.log(System.Logger.Level.DEBUG, "Resolved fullPath: " + fullPath);

        return fullPath.toString();
    }
}
