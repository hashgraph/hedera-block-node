package com.hedera.block.server.persistence.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class BNLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

    private final long maxEntries;
//    private final Logger logger = Logger.getLogger(BNLinkedHashMap.class.getName());

    BNLinkedHashMap(long maxEntries) {
        this.maxEntries = maxEntries;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {

        if (size() > maxEntries) {
//            logger.debug(String.format("Removing eldest entry: %s", eldest));
            return true;
        }

        return false;
//        return size() > maxEntries;
    }
}
