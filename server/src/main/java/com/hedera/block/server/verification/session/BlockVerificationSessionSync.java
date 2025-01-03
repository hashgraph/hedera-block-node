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

import static com.hedera.block.server.verification.hasher.HashingUtilities.getBlockItemHash;

import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.hasher.NaiveStreamingTreeHasher;
import com.hedera.block.server.verification.signature.SignatureVerifier;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;

/**
 * A synchronous implementation of the BlockVerificationSession. It processes the block items
 * synchronously in the calling thread.
 */
public class BlockVerificationSessionSync extends AbstractBlockVerificationSession {

    /**
     * The logger for this class.
     */
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    /**
     * Constructs a synchronous block verification session.
     *
     * @param blockHeader        the header of the block being verified
     * @param metricsService     the service to observe metrics
     * @param signatureVerifier  the signature verifier
     */
    public BlockVerificationSessionSync(
            @NonNull final BlockHeader blockHeader,
            @NonNull final MetricsService metricsService,
            @NonNull final SignatureVerifier signatureVerifier) {

        super(
                blockHeader,
                metricsService,
                signatureVerifier,
                new NaiveStreamingTreeHasher(),
                new NaiveStreamingTreeHasher());
    }

    @Override
    public void appendBlockItems(List<BlockItemUnparsed> blockItems) {
        if (!isRunning()) {
            LOGGER.log(System.Logger.Level.ERROR, "Block verification session is not running");
            return;
        }

        try {
            processBlockItems(blockItems);
        } catch (Exception ex) {
            handleProcessingError(ex);
        }
    }

    @Override
    protected void processBlockItems(List<BlockItemUnparsed> blockItems) throws ParseException {
        for (BlockItemUnparsed item : blockItems) {
            final BlockItemUnparsed.ItemOneOfType kind = item.item().kind();
            switch (kind) {
                case EVENT_HEADER, EVENT_TRANSACTION -> inputTreeHasher.addLeaf(getBlockItemHash(item));
                case TRANSACTION_RESULT, TRANSACTION_OUTPUT, STATE_CHANGES -> outputTreeHasher.addLeaf(
                        getBlockItemHash(item));
            }
        }

        // Check if this batch contains the final block proof
        final BlockItemUnparsed lastItem = blockItems.getLast();
        if (lastItem.hasBlockProof()) {
            BlockProof blockProof = BlockProof.PROTOBUF.parse(lastItem.blockProof());
            finalizeVerification(blockProof);
        }
    }
}
