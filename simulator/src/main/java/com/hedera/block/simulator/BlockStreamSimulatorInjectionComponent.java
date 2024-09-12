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

import com.hedera.block.simulator.config.ConfigInjectionModule;
import com.hedera.block.simulator.generator.GeneratorInjectionModule;
import com.hedera.block.simulator.grpc.GrpcInjectionModule;
import com.swirlds.config.api.Configuration;
import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;

/** The component used to inject the block stream simulator into the application. */
@Singleton
@Component(
        modules = {
            ConfigInjectionModule.class,
            GeneratorInjectionModule.class,
            GrpcInjectionModule.class,
        })
public interface BlockStreamSimulatorInjectionComponent {

    /**
     * Gets the block stream simulator.
     *
     * @return the block stream simulator
     */
    BlockStreamSimulatorApp getBlockStreamSimulatorApp();

    /** The factory used to create the block stream simulator injection component. */
    @Component.Factory
    interface Factory {
        /**
         * Creates the block stream simulator injection component.
         *
         * @param configuration the configuration to be used by the block stream simulator
         * @return the block stream simulator injection component
         */
        BlockStreamSimulatorInjectionComponent create(@BindsInstance Configuration configuration);
    }
}
