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

import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import com.swirlds.config.extensions.sources.SystemPropertiesConfigSource;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigProviderImpl implements ConfigProvider {
    private static final System.Logger LOGGER =
            System.getLogger(ConfigProviderImpl.class.getName());
    private final Configuration configuration;

    public ConfigProviderImpl() {
        final var builder = createConfigurationBuilder();
        //        addFileSources(builder, useGenesisSource);
        //        if (overrideValues != null) {
        //            overrideValues.forEach(builder::withValue);
        //        }
        configuration = builder.build();
    }

    @NonNull
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    private ConfigurationBuilder createConfigurationBuilder() {
        try {
            return ConfigurationBuilder.create()
                    .withSource(SystemEnvironmentConfigSource.getInstance())
                    .withSource(SystemPropertiesConfigSource.getInstance())
                    .withSource(new ClasspathFileConfigSource(Path.of("app.properties")))
                    .autoDiscoverExtensions();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
