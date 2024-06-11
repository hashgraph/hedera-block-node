package com.hedera.block.server.persistence.cache;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

@Singleton
public class LRUCache implements BlockCache {

    private final Map<Long, BlockStreamServiceGrpcProto.Block> m;
    private final Logger logger = Logger.getLogger("LRUCache");

    public LRUCache(long maxEntries) {
        logger.info("Creating LRUCache with maxEntries: " + maxEntries);
        m = Collections.synchronizedMap(new BNLinkedHashMap<>(maxEntries));
    }

    @Override
    public Long insert(BlockStreamServiceGrpcProto.Block block) {
        ///---
        long id = block.getId();
//        logger.debug("Caching block: " + id);

        m.putIfAbsent(id, block);

//        logger.debug("Cached block: " + id);
        return id;
    }

    @Override
    public BlockStreamServiceGrpcProto.Block get(Long id) {
        return m.get(id);
    }

    @Override
    public boolean contains(Long id) {
        return m.containsKey(id);
    }
}
