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

package com.hedera.block.simulator;

import static java.lang.System.Logger.Level.INFO;

import com.hedera.pbj.runtime.ParseException;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.api.ConfigurationBuilder;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import com.swirlds.config.extensions.sources.SystemEnvironmentConfigSource;
import com.swirlds.config.extensions.sources.SystemPropertiesConfigSource;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.file.Path;

/** The BlockStreamSimulator class defines the simulator for the block stream. */
public class BlockStreamSimulator {
    private static final Logger LOGGER = System.getLogger(BlockStreamSimulator.class.getName());

    /** This constructor should not be instantiated. */
    private BlockStreamSimulator() {}

    /**
     * The main entry point for the block stream simulator.
     *
     * @param args the arguments to be passed to the block stream simulator
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    public static void main(String[] args)
            throws IOException, InterruptedException, ParseException {

        LOGGER.log(INFO, "Starting Block Stream Simulator");

        ConfigurationBuilder configurationBuilder =
                ConfigurationBuilder.create()
                        .withSource(SystemEnvironmentConfigSource.getInstance())
                        .withSource(SystemPropertiesConfigSource.getInstance())
                        .withSource(new ClasspathFileConfigSource(Path.of("app.properties")))
                        .autoDiscoverExtensions();

        Configuration configuration = configurationBuilder.build();

        BlockStreamSimulatorInjectionComponent DIComponent =
                DaggerBlockStreamSimulatorInjectionComponent.factory().create(configuration);

        BlockStreamSimulatorApp blockStreamSimulatorApp = DIComponent.getBlockStreamSimulatorApp();
        blockStreamSimulatorApp.start();
    }
}
