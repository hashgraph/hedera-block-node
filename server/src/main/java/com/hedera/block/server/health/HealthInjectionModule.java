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

package com.hedera.block.server.health;

import dagger.Binds;
import dagger.Module;
import javax.inject.Singleton;

/**
 * A Dagger module for providing dependencies for Health Module, should we refactor to have an
 * observability module instead?.
 */
@Module
public interface HealthInjectionModule {

    /**
     * Binds the health service to the health service implementation.
     *
     * @param healthService needs a health service implementation
     * @return the health service implementation
     */
    @Singleton
    @Binds
    HealthService bindHealthService(HealthServiceImpl healthService);
}
