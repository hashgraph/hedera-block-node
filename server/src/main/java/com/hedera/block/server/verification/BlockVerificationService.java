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

package com.hedera.block.server.verification;

import static java.lang.System.Logger.Level.WARNING;

import com.hedera.block.server.metrics.BlockNodeMetricTypes;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.signature.SignatureVerifier;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

public class BlockVerificationService {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final MetricsService metricsService;
    private final SignatureVerifier signatureVerifier;

    private BlockVerificationSession currentSession;
    private Bytes lastBlockHash = Bytes.EMPTY;

    @Inject
    public BlockVerificationService(
            @NonNull final MetricsService metricsService,
            @NonNull final SignatureVerifier signatureVerifier) {
        this.metricsService = metricsService;
        this.signatureVerifier = signatureVerifier;
    }

    public void onBlockItemsReceived(List<BlockItemUnparsed> blockItems) throws ParseException, ExecutionException, InterruptedException {

        final BlockItemUnparsed firstItem = blockItems.getFirst();

        // If we have a new block header, that means a new block has started
        if (firstItem.hasBlockHeader()) {
            metricsService
                    .get(BlockNodeMetricTypes.Counter.VerificationBlocksReceived)
                    .increment();
            BlockHeader blockHeader = BlockHeader.PROTOBUF.parse(firstItem.blockHeader());
            // double check last block hash with prev of current block
            if(currentSession != null) {
                lastBlockHash = currentSession.getVerificationResult().get().blockHash();

                    if (!lastBlockHash.equals(blockHeader.previousBlockHash())) {
                        LOGGER.log(WARNING, "Block header previous hash does not match last calculated block hash.");
                        metricsService
                                .get(BlockNodeMetricTypes.Counter.VerificationBlocksFailed)
                                .increment();
                    }

            } else {
                LOGGER.log(WARNING, "No previous session to compare block hashes.");
            }

            // start new session
            currentSession = new BlockVerificationSessionImpl(blockHeader, blockItems, metricsService, signatureVerifier);

        } else {
            if (currentSession == null) {
                LOGGER.log(WARNING, "Received block items before a block header. Ignoring.");
                return;
            }
            currentSession.appendBlockItems(blockItems);
        }
    }
}
