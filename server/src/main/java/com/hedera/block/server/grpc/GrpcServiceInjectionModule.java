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

package com.hedera.block.server.grpc;

import dagger.Binds;
import dagger.Module;
import javax.inject.Singleton;

/**
 * A Dagger Module for GRPC services that are at the BlockNode Services.
 */
@Module
public interface GrpcServiceInjectionModule {

    /**
     * Provides a block stream service singleton using DI.
     *
     * @param blockStreamService should come from DI
     * @return a block stream service singleton
     */
    @Singleton
    @Binds
    BlockStreamService bindBlockStreamService(BlockStreamService blockStreamService);

    /**
     * Provides a block access service singleton using DI.
     *
     * @param blockAccessService should come from DI
     * @return a block access service singleton
     */
    @Singleton
    @Binds
    BlockAccessService bindBlockAccessService(BlockAccessService blockAccessService);
}
