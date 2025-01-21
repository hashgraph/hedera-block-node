// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.config.logging;

import com.swirlds.config.api.source.ConfigSource;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

class TestConfigSource implements ConfigSource {

    private final Map<String, String> config;

    TestConfigSource(final Map<String, String> config) {
        this.config = config;
    }

    @NonNull
    @Override
    public Set<String> getPropertyNames() {
        return config.keySet();
    }

    @Nullable
    @Override
    public String getValue(@NonNull String s) throws NoSuchElementException {
        return config.get(s);
    }

    @Override
    public boolean isListProperty(@NonNull String s) throws NoSuchElementException {
        return false;
    }

    @NonNull
    @Override
    public List<String> getListValue(@NonNull String s) throws NoSuchElementException {
        return List.of();
    }
}
