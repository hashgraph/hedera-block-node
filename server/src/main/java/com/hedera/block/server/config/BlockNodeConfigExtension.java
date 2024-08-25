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

package com.hedera.block.server.config;

import com.google.auto.service.AutoService;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.swirlds.common.metrics.config.MetricsConfig;
import com.swirlds.common.metrics.platform.prometheus.PrometheusConfig;
import com.swirlds.config.api.ConfigurationExtension;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;

/** Registers configuration types for the server. */
@AutoService(ConfigurationExtension.class)
public class BlockNodeConfigExtension implements ConfigurationExtension {

    /** Explicitly defined constructor. */
    public BlockNodeConfigExtension() {
        super();
    }

    /**
     * {@inheritDoc}
     *
     * @return Set of configuration data types for the server
     */
    @NonNull
    @Override
    public Set<Class<? extends Record>> getConfigDataTypes() {
        return Set.of(
                MetricsConfig.class,
                PrometheusConfig.class,
                ConsumerConfig.class,
                PersistenceStorageConfig.class);
    }
}
