/*
 * Copyright (C) 2024-2025 Hedera Hashgraph, LLC
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

package com.hedera.block.server.verification.session;

import com.hedera.block.server.verification.VerificationResult;
import com.hedera.hapi.block.BlockItemUnparsed;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Defines the contract for a block verification session.
 */
public interface BlockVerificationSession {

    /**
     * Append new block items to be processed by this verification session.
     *
     * @param blockItems the list of block items to process.
     */
    void appendBlockItems(List<BlockItemUnparsed> blockItems);

    /**
     * Indicates whether the verification session is still running.
     *
     * @return true if running; false otherwise.
     */
    boolean isRunning();

    /**
     * Returns a future that completes with the verification result of the entire block
     * once verification is complete.
     *
     * @return a CompletableFuture for the verification result.
     */
    CompletableFuture<VerificationResult> getVerificationResult();
}
