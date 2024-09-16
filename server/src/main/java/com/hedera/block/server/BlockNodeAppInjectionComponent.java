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

package com.hedera.block.server;

import com.hedera.block.server.config.ConfigInjectionModule;
import com.hedera.block.server.health.HealthInjectionModule;
import com.hedera.block.server.mediator.MediatorInjectionModule;
import com.hedera.block.server.metrics.MetricsInjectionModule;
import com.hedera.block.server.notifier.NotifierInjectionModule;
import com.hedera.block.server.persistence.PersistenceInjectionModule;
import com.hedera.block.server.service.ServiceInjectionModule;
import com.hedera.block.server.verifier.VerifierInjectionModule;
import com.swirlds.config.api.Configuration;
import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;

/** The infrastructure used to manage the instances and inject them using Dagger */
@Singleton
@Component(
        modules = {
            NotifierInjectionModule.class,
            ServiceInjectionModule.class,
            BlockNodeAppInjectionModule.class,
            HealthInjectionModule.class,
            PersistenceInjectionModule.class,
            MediatorInjectionModule.class,
            VerifierInjectionModule.class,
            ConfigInjectionModule.class,
            MetricsInjectionModule.class,
        })
public interface BlockNodeAppInjectionComponent {
    /**
     * Get the block node app server.
     *
     * @return the block node app server
     */
    BlockNodeApp getBlockNodeApp();

    /**
     * Factory for the block node app injection component, needs a configuration to create the
     * component and the block node app with all the wired dependencies.
     */
    @Component.Factory
    interface Factory {
        /**
         * Create the block node app injection component.
         *
         * @param configuration the configuration
         * @return the block node app injection component
         */
        BlockNodeAppInjectionComponent create(@BindsInstance Configuration configuration);
    }
}
