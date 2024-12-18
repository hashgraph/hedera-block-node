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

package com.hedera.block.simulator.grpc;

import com.hedera.block.simulator.grpc.impl.ConsumerStreamGrpcClientImpl;
import com.hedera.block.simulator.grpc.impl.PublishStreamGrpcClientImpl;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Singleton;

/** The module used to inject the gRPC client. */
@Module
public interface GrpcInjectionModule {

    /**
     * Binds the PublishStreamGrpcClient to the PublishStreamGrpcClientImpl.
     *
     * @param publishStreamGrpcClient the PublishStreamGrpcClientImpl
     * @return the PublishStreamGrpcClient
     */
    @Singleton
    @Binds
    PublishStreamGrpcClient bindPublishStreamGrpcClient(PublishStreamGrpcClientImpl publishStreamGrpcClient);

    /**
     * Binds the ConsumerStreamGrpcClient to the ConsumerStreamGrpcClientImpl.
     *
     * @param consumerStreamGrpcClient the ConsumerStreamGrpcClientImpl
     * @return the ConsumerStreamGrpcClient
     */
    @Singleton
    @Binds
    ConsumerStreamGrpcClient bindConsumerStreamGrpcClient(ConsumerStreamGrpcClientImpl consumerStreamGrpcClient);

    /**
     * Provides the stream enabled flag
     *
     * @return the stream enabled flag
     */
    @Singleton
    @Provides
    static AtomicBoolean provideStreamEnabledFlag() {
        return new AtomicBoolean(true);
    }
}
