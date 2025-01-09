// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.verification.service;

import static java.lang.System.Logger.Level.WARNING;
import static java.util.Objects.requireNonNull;

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

    /**
     * Constructs a new BlockVerificationServiceImpl.
     *
     * @param metricsService the metrics service
     * @param sessionFactory the session factory
     */
    @Inject
    public BlockVerificationServiceImpl(
            @NonNull final MetricsService metricsService,
            @NonNull final BlockVerificationSessionFactory sessionFactory) {
        this.metricsService = requireNonNull(metricsService);
        this.sessionFactory = requireNonNull(sessionFactory);
    }

    /**
     * Everytime the handler receives block items, it will call this method to verify the block items.
     *
     * @param blockItems the block items to add to the verification service
     * @throws ParseException if the block items are invalid
     */
    @Override
    public void onBlockItemsReceived(@NonNull List<BlockItemUnparsed> blockItems) throws ParseException {

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
