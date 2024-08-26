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

import com.hedera.block.simulator.config.ConfigProvider;
import com.hedera.block.simulator.config.ConfigProviderImpl;
import com.hedera.block.simulator.config.data.GrpcConfig;
import java.lang.System.Logger;

public class BlockStreamSimulator {
    private static final Logger LOGGER =
            System.getLogger(BlockStreamSimulator.class.getName());

    public BlockStreamSimulator() {}

    public static void main(String[] args) {
        BlockStreamSimulator blockStreamSimulator = new BlockStreamSimulator();
        blockStreamSimulator.start();
    }

    public void start() {
        ConfigProvider configProvider = new ConfigProviderImpl();
        LOGGER.log(Logger.Level.INFO, "Starting Block Stream Simulator");
    }

    public void stop() {

    }
}
