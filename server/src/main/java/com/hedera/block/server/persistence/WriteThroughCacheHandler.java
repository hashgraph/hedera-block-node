package com.hedera.block.server.persistence;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.persistence.cache.BlockCache;
import com.hedera.block.server.persistence.storage.BlockStorage;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;

public class WriteThroughCacheHandler implements BlockPersistenceHandler {

    private final BlockStorage blockStorage;
    private final BlockCache blockCache;
//    private final Logger logger = Logger.getLogger(WriteThroughCacheHandler.class);

    public WriteThroughCacheHandler(BlockStorage blockStorage,
                                    BlockCache blockCache) {
        this.blockStorage = blockStorage;
        this.blockCache = blockCache;
    }

    @Override
    public Long persist(BlockStreamServiceGrpcProto.Block block) {

        // Write-Through cache
        blockStorage.write(block);
        return blockCache.insert(block);
    }

    @Override
    public Queue<BlockStreamServiceGrpcProto.Block> readFrom(Long id, Predicate<BlockStreamServiceGrpcProto.Block> filter) {
        Queue<BlockStreamServiceGrpcProto.Block> blocks = new ArrayDeque<>();

        long count = id;
        Optional<BlockStreamServiceGrpcProto.Block> blockOpt = read(count);
        while (blockOpt.isPresent()) {

            BlockStreamServiceGrpcProto.Block block = blockOpt.get();
            if (filter.test(block)) {
                blocks.offer(block);
            }

            blockOpt = read(++count);
        }

        return blocks;
    }

    @Override
    public Optional<BlockStreamServiceGrpcProto.Block> read(Long id) {

        if (blockCache.contains(id)) {
//            logger.debug("Cache hit on: " + id);
            return Optional.of(blockCache.get(id));
        } else {
            // Update the cache
//            logger.debug("Cache miss on: " + id);
            Optional<BlockStreamServiceGrpcProto.Block> block = blockStorage.read(id);
            block.ifPresent(blockCache::insert);

            return block;
        }
    }
}
