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

import static com.hedera.block.server.verification.hasher.CommonUtils.HASH_SIZE;
import static com.hedera.block.server.verification.hasher.CommonUtils.combine;
import static com.hedera.block.server.verification.hasher.CommonUtils.sha384DigestOrThrow;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.metrics.BlockNodeMetricTypes;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.hasher.NaiveStreamingTreeHasher;
import com.hedera.block.server.verification.hasher.StreamingTreeHasher;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import javax.inject.Inject;

public class BlockHashService {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final MetricsService metricsService;

    private final ExecutorService executor = ForkJoinPool.commonPool();
    private final int hashCombineBatchSize = 32;

    private long currentBlockNumber = -1;
    private Bytes lastBlockHash;
    private StreamingTreeHasher inputTreeHasher;
    private StreamingTreeHasher outputTreeHasher;

    private long blockWorkStartTime = 0L;

    @Inject
    public BlockHashService(@NonNull final MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public void onBlockItemsReceived(List<BlockItemUnparsed> blockItems)
            throws ParseException, ExecutionException, InterruptedException {

        final BlockItemUnparsed firstItem = blockItems.getFirst();
        if (firstItem.hasBlockHeader()) {
            metricsService
                    .get(BlockNodeMetricTypes.Counter.VerificationBlocksReceived)
                    .increment();
            BlockHeader blockHeader = BlockHeader.PROTOBUF.parse(firstItem.blockHeader());
            currentBlockNumber = Preconditions.requireWhole(blockHeader.number());
            inputTreeHasher = new NaiveStreamingTreeHasher();
            outputTreeHasher = new NaiveStreamingTreeHasher();
            blockWorkStartTime = System.nanoTime();
            LOGGER.log(INFO, "Starting new Hash for block number: " + blockHeader.number());

            if (lastBlockHash != blockHeader.previousBlockHash()) {
                metricsService
                        .get(BlockNodeMetricTypes.Counter.VerificationBlocksFailed)
                        .increment();
            }
        }

        LOGGER.log(INFO, "Working on batch size: " + blockItems.size());

        for (BlockItemUnparsed item : blockItems) {
            final BlockItemUnparsed.ItemOneOfType kind = item.item().kind();
            switch (kind) {
                case EVENT_HEADER, EVENT_TRANSACTION -> inputTreeHasher.addLeaf(getBlockItemHash(item));
                case TRANSACTION_RESULT, TRANSACTION_OUTPUT, STATE_CHANGES -> outputTreeHasher.addLeaf(
                        getBlockItemHash(item));
            }
        }

        // process block items hashes.
        final BlockItemUnparsed lastItem = blockItems.getLast();
        if (lastItem.hasBlockProof()) {

            Bytes inputHash = inputTreeHasher.rootHash().get();
            Bytes outputHash = outputTreeHasher.rootHash().get();

            // Parse BlockProof.
            BlockProof blockProof = BlockProof.PROTOBUF.parse(lastItem.blockProof());
            Bytes providedLasBlockHash = blockProof.previousBlockRootHash();
            Bytes providedBlockStartStateHash = blockProof.startOfBlockStateRootHash();

            final var leftParent = combine(providedLasBlockHash, inputHash);
            final var rightParent = combine(outputHash, providedBlockStartStateHash);
            final var blockHash = combine(leftParent, rightParent);

            // set to last block hash for next block verification.
            this.lastBlockHash = blockHash;

            // Call SignatureVerifier to verify the signature of the block.
            // SignatureVerifier.verify(blockProof.signature(), blockHash.toByteArray());

            // Call the BlockStatusService to notify the block status.

            metricsService
                    .get(BlockNodeMetricTypes.Counter.VerificationBlocksVerified)
                    .increment();
        }

        long elapsedTime = System.nanoTime() - blockWorkStartTime;
        metricsService.get(BlockNodeMetricTypes.Counter.VerificationBlockTime).add(elapsedTime);
    }

    private ByteBuffer getBlockItemHash(BlockItemUnparsed blockItemUnparsed) {
        final var digest = sha384DigestOrThrow();
        ByteBuffer buffer = ByteBuffer.allocate(HASH_SIZE);
        buffer.put(digest.digest(
                BlockItemUnparsed.PROTOBUF.toBytes(blockItemUnparsed).toByteArray()));
        buffer.flip();
        return buffer;
    }
}
