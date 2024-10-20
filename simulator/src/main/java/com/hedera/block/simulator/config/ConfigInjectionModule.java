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

package com.hedera.block.simulator.config;

import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.swirlds.config.api.Configuration;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/** The module used to inject the configuration data into the application. */
@Module
public interface ConfigInjectionModule {

    /**
     * Provides the block stream configuration.
     *
     * @param configuration the configuration to be used by the block stream
     * @return the block stream configuration
     */
    @Singleton
    @Provides
    static BlockStreamConfig provideBlockStreamConfig(Configuration configuration) {
        return configuration.getConfigData(BlockStreamConfig.class);
    }

    /**
     * Provides the gRPC configuration.
     *
     * @param configuration the configuration to be used by the gRPC
     * @return the gRPC configuration
     */
    @Singleton
    @Provides
    static GrpcConfig provideGrpcConfig(Configuration configuration) {
        return configuration.getConfigData(GrpcConfig.class);
    }

    /**
     * Provides the block generator configuration.
     *
     * @param configuration the configuration to be used by the block generator
     * @return the block generator configuration
     */
    @Singleton
    @Provides
    static BlockGeneratorConfig provideBlockGeneratorConfig(Configuration configuration) {
        return configuration.getConfigData(BlockGeneratorConfig.class);
    }
}
