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
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.stream.BlockItem;
import dagger.Module;
import dagger.Provides;
import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Singleton;

/** A Dagger module for providing dependencies for Validator Module. */
@Module
public interface ValidatorInjectionModule {

    /**
     * Provides a stream validator builder singleton using the block writer, block node context, and
     * service status.
     *
     * @param blockWriter the block writer
     * @param blockNodeContext the application context
     * @param serviceStatus the service status
     * @return a stream validator builder singleton
     */
    @Provides
    @Singleton
    static StreamVerifierBuilder providesStreamValidatorBuilder(
            @NonNull final BlockWriter<BlockItem> blockWriter,
            @NonNull final BlockNodeContext blockNodeContext,
            @NonNull final ServiceStatus serviceStatus) {
        return StreamVerifierBuilder.newBuilder(blockWriter, blockNodeContext, serviceStatus);
    }
}
