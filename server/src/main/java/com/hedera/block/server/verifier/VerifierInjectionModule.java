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

package com.hedera.block.server.verifier;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Singleton;

/** A Dagger module for providing dependencies for Validator Module. */
@Module
public interface VerifierInjectionModule {

    @Provides
    @Singleton
    static BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>>
            providesBlockNodeEventHandler(
                    @NonNull final Notifier notifier,
                    @NonNull final BlockWriter<BlockItem> blockWriter,
                    @NonNull final BlockNodeContext blockNodeContext,
                    @NonNull final ServiceStatus serviceStatus) {
        return StreamVerifierBuilder.newBuilder(
                        notifier, blockWriter, blockNodeContext, serviceStatus)
                .build();
    }
}
