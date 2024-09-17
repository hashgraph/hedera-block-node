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

package com.hedera.block.server.notifier;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.service.ServiceStatus;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Singleton;

/** A Dagger module for providing dependencies for Notifier Module. */
@Module
public interface NotifierInjectionModule {

    /**
     * Provides the notifier.
     *
     * @param streamMediator the stream mediator
     * @param blockNodeContext the block node context
     * @param serviceStatus the service status
     * @return the notifier
     */
    @Provides
    @Singleton
    static Notifier providesNotifier(
            @NonNull final LiveStreamMediator streamMediator,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
        return NotifierBuilder.newBuilder(streamMediator, blockNodeContext, serviceStatus).build();
    }
}
