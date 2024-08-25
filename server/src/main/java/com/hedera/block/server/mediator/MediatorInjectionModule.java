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

import com.hedera.block.server.ServiceStatus;
import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.data.ObjectEvent;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
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
     * @param blockWriter the block writer
     * @param blockNodeContext the block node context
     * @param serviceStatus the service status
     * @return the stream mediator
     */
    @Provides
    @Singleton
    static StreamMediator<BlockItem, ObjectEvent<SubscribeStreamResponse>> providesStreamMediator(
            @NonNull BlockWriter<BlockItem> blockWriter,
            @NonNull BlockNodeContext blockNodeContext,
            @NonNull ServiceStatus serviceStatus) {
        return LiveStreamMediatorBuilder.newBuilder(blockWriter, blockNodeContext, serviceStatus)
                .build();
    }
}