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

package com.hedera.block.server.verification.service;

import static java.lang.System.Logger.Level.WARNING;

import com.hedera.block.server.metrics.BlockNodeMetricTypes;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.session.BlockVerificationSession;
import com.hedera.block.server.verification.session.BlockVerificationSessionFactory;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import javax.inject.Inject;

/**
 * Service that handles the verification of block items, it receives items from the handler.
 */
public class BlockVerificationServiceImpl implements BlockVerificationService {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final MetricsService metricsService;

    private final BlockVerificationSessionFactory sessionFactory;
    private BlockVerificationSession currentSession;

    @Inject
    public BlockVerificationServiceImpl(
            @NonNull final MetricsService metricsService,
            @NonNull final BlockVerificationSessionFactory sessionFactory) {
        this.metricsService = metricsService;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void onBlockItemsReceived(List<BlockItemUnparsed> blockItems) throws ParseException {

        final BlockItemUnparsed firstItem = blockItems.getFirst();

        // If we have a new block header, that means a new block has started
        if (firstItem.hasBlockHeader()) {
            metricsService
                    .get(BlockNodeMetricTypes.Counter.VerificationBlocksReceived)
                    .increment();
            BlockHeader blockHeader = BlockHeader.PROTOBUF.parse(firstItem.blockHeader());

            // double check last block hash with prev of current block
            if (currentSession != null) {
                currentSession.getVerificationResult().thenAccept(result -> {
                    if (!result.blockHash().equals(blockHeader.previousBlockHash())) {
                        LOGGER.log(WARNING, "blockHeader.previousBlockHash does not match calculated previous hash.");
                        metricsService
                                .get(BlockNodeMetricTypes.Counter.VerificationBlocksFailed)
                                .increment();
                    }
                });
            } else {
                LOGGER.log(WARNING, "No previous session to compare block hashes.");
            }

            // start new session and set it as current
            currentSession = sessionFactory.createSession(blockHeader);
            currentSession.appendBlockItems(blockItems);

        } else { // If we don't have a block header, we should have a current session, otherwise ignore
            if (currentSession == null) {
                LOGGER.log(WARNING, "Received block items before a block header. Ignoring.");
                return;
            }
            // Append to current session
            currentSession.appendBlockItems(blockItems);
        }
    }
}