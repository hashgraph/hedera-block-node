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

package com.hedera.block.server.mediator;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.notifier.Notifiable;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.SubscribeStreamResponse;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Singleton;

/** A Dagger module for providing dependencies for Mediator Module.` */
@Module
public interface MediatorInjectionModule {

    /**
     * Provides the stream mediator.
     *
     * @param blockNodeContext the block node context
     * @param serviceStatus the service status
     * @return the stream mediator
     */
    @Provides
    @Singleton
    static LiveStreamMediator providesLiveStreamMediator(
            @NonNull BlockNodeContext blockNodeContext, @NonNull ServiceStatus serviceStatus) {
        final String mediatorType = blockNodeContext
                .configuration()
                .getConfigData(MediatorConfig.class)
                .type();
        if ("NOOP".equals(mediatorType)) {
            return new NoOpLiveStreamMediator(blockNodeContext);
        }

        return LiveStreamMediatorBuilder.newBuilder(blockNodeContext, serviceStatus)
                .build();
    }

    /**
     * Binds the subscription handler to the live stream mediator.
     *
     * @param liveStreamMediator the live stream mediator
     * @return the subscription handler
     */
    @Binds
    @Singleton
    SubscriptionHandler<SubscribeStreamResponse> bindSubscriptionHandler(
            @NonNull final LiveStreamMediator liveStreamMediator);

    /**
     * Binds the mediator to the notifiable interface.
     *
     * @param liveStreamMediator the live stream mediator
     * @return the notifiable interface
     */
    @Binds
    @Singleton
    Notifiable bindMediator(@NonNull final LiveStreamMediator liveStreamMediator);
}
