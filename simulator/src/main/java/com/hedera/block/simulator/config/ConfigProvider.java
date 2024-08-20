package com.hedera.block.simulator.config;

import com.swirlds.config.api.Configuration;
import edu.umd.cs.findbugs.annotations.NonNull;

public interface ConfigProvider {
    @NonNull
    Configuration getConfiguration();
}
