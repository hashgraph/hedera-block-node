package com.hedera.block.server.persistence.storage;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

public class FileSystemBlockStorage implements BlockStorage {

    private final Path path;
    private final Logger logger = Logger.getLogger("FileSystemBlockStorage");

    public FileSystemBlockStorage(Path path) {
        this.path = path;
        logger.info("Block Node Root Path: " + path);
    }

    @Override
    public Optional<Long> write(BlockStreamServiceGrpcProto.Block block) {
        Long id = block.getId();
        String fullPath = resolvePath(id);
        logger.fine("Wrote the file: " + fullPath);

        try (FileOutputStream fos = new FileOutputStream(fullPath)) {
            block.writeTo(fos);
            return Optional.of(id);
        }
        catch (IOException e) {
            logger.severe("Error writing string to file: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<BlockStreamServiceGrpcProto.Block> read(Long id) {
        return read(resolvePath(id));
    }

    private Optional<BlockStreamServiceGrpcProto.Block> read(String filePath) {

        try (FileInputStream fis = new FileInputStream(filePath)) {
            return Optional.of(BlockStreamServiceGrpcProto.Block.parseFrom(fis));
        } catch (FileNotFoundException io) {
//            logger.severe("Error reading file: " + filePath);
//            throw new RuntimeException("Error reading file: " + filePath, io);
            return Optional.empty();
        } catch (IOException io) {
            throw new RuntimeException("Error reading file: " + filePath, io);
        }
    }

    private String resolvePath(Long id) {
        String fileName = id + ".txt";
        logger.fine("resolved filename: " + fileName);

        Path fullPath = path.resolve(fileName);
        logger.fine("resolved fullPath: " + fullPath);

        return fullPath.toString();
    }

}
