// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.grpc;

import com.hedera.block.simulator.grpc.impl.ConsumerStreamGrpcClientImpl;
import com.hedera.block.simulator.grpc.impl.PublishStreamGrpcClientImpl;
import com.hedera.block.simulator.grpc.impl.PublishStreamGrpcServerImpl;
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
     * Binds the PublishStreamGrpcServer to the PublishStreamGrpcServerImpl.
     *
     * @param PublishStreamGrpcServer the PublishStreamGrpcServerImpl
     * @return the ConsumerStreamGrpcClient
     */
    @Singleton
    @Binds
    PublishStreamGrpcServer bindPublishStreamGrpcServer(PublishStreamGrpcServerImpl PublishStreamGrpcServer);

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
