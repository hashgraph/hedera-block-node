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

import static com.hedera.block.server.verification.hasher.CommonUtils.getBlockItemHash;
import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.server.metrics.BlockNodeMetricTypes;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.hasher.CommonUtils;
import com.hedera.block.server.verification.hasher.ConcurrentStreamingTreeHasher;
import com.hedera.block.server.verification.hasher.StreamingTreeHasher;
import com.hedera.block.server.verification.signature.SignatureVerifier;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class BlockVerificationSessionAsync implements BlockVerificationSession {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());
    private final MetricsService metricsService;
    private final SignatureVerifier signatureVerifier;
    private final ExecutorService executorService;
    private final int hashCombineBatchSize = 32;

    private final long blockNumber;
    private final ExecutorService executor;
    private final StreamingTreeHasher inputTreeHasher;
    private final StreamingTreeHasher outputTreeHasher;
    private final long blockWorkStartTime;

    private volatile boolean running = true;

    private final CompletableFuture<VerificationResult> verificationResultFuture = new CompletableFuture<>();

    public BlockVerificationSessionAsync(
            @NonNull final BlockHeader blockHeader,
            @NonNull final List<BlockItemUnparsed> initialBlockItems,
            @NonNull final MetricsService metricsService,
            @NonNull final SignatureVerifier signatureVerifier) {

        this.blockNumber = blockHeader.number();
        this.metricsService = metricsService;
        this.signatureVerifier = signatureVerifier;

        executorService = ForkJoinPool.commonPool();

        // ASYNC
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "block-verification-session-" + blockNumber);
            t.setDaemon(true);
            return t;
        });

        inputTreeHasher = new ConcurrentStreamingTreeHasher(executorService, hashCombineBatchSize);
        outputTreeHasher = new ConcurrentStreamingTreeHasher(executorService, hashCombineBatchSize);

        blockWorkStartTime = System.nanoTime();
        appendBlockItems(initialBlockItems);
    }

    @Override
    public void appendBlockItems(List<BlockItemUnparsed> blockItems) {

        if (!running) {
            LOGGER.log(System.Logger.Level.ERROR, "Block verification session is not running");
            return;
        }
        // ASYNC
        Callable<Void> task = () -> {
            try {
                processBlockItems(blockItems);
            } catch (ParseException | ExecutionException | InterruptedException ex) {
                LOGGER.log(System.Logger.Level.ERROR, "Error processing block items", ex);
                metricsService
                        .get(BlockNodeMetricTypes.Counter.VerificationBlocksError)
                        .increment();
                verificationResultFuture.completeExceptionally(ex);
            }
            return null;
        };
        executor.submit(task);
    }

    private void processBlockItems(List<BlockItemUnparsed> blockItems)
            throws ParseException, ExecutionException, InterruptedException {
        for (BlockItemUnparsed item : blockItems) {
            final BlockItemUnparsed.ItemOneOfType kind = item.item().kind();
            switch (kind) {
                case EVENT_HEADER, EVENT_TRANSACTION -> inputTreeHasher.addLeaf(getBlockItemHash(item));
                case TRANSACTION_RESULT, TRANSACTION_OUTPUT, STATE_CHANGES -> outputTreeHasher.addLeaf(
                        getBlockItemHash(item));
            }
        }

        // check if is final batch
        final BlockItemUnparsed lastItem = blockItems.getLast();
        if (lastItem.hasBlockProof()) {
            BlockProof blockProof = BlockProof.PROTOBUF.parse(lastItem.blockProof());
            finalizeVerification(blockProof);
        }
    }


    private void finalizeVerification(BlockProof blockProof) throws ExecutionException, InterruptedException {
        // compute the final block hash
        Bytes blockHash = CommonUtils.computeFinalBlockHash(blockProof, inputTreeHasher, outputTreeHasher);
        // Verify the block hash against the signature.
        Boolean verified = signatureVerifier.verifySignature(blockHash, blockProof.blockSignature());
        if (verified) {
            verificationResultFuture.complete(new VerificationResult(blockNumber, blockHash, BlockVerificationStatus.VERIFIED));

            // log metrics
            long verificationLatency = System.nanoTime() - blockWorkStartTime;
            metricsService
                    .get(BlockNodeMetricTypes.Counter.VerificationBlockTime)
                    .add(verificationLatency);
            metricsService
                    .get(BlockNodeMetricTypes.Counter.VerificationBlocksVerified)
                    .increment();
        } else {
            LOGGER.log(INFO, "Block verification failed for block number: {0}", blockNumber);
            metricsService
                    .get(BlockNodeMetricTypes.Counter.VerificationBlocksFailed)
                    .increment();
            verificationResultFuture.complete(new VerificationResult(blockNumber, blockHash, BlockVerificationStatus.SIGNATURE_INVALID));
        }

        // finish processing
        shutdownSession();
    }

    /**
     * Shut down this session after the block is fully processed.
     */
    private void shutdownSession() {
        running = false;
        executor.shutdown();
    }

    public boolean isRunning() {
        return running;
    }

    public CompletableFuture<VerificationResult> getVerificationResult() {
        return verificationResultFuture;
    }
}
