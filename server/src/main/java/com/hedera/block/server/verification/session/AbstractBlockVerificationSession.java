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

package com.hedera.block.server.verification.session;

import static java.lang.System.Logger.Level.INFO;

import com.hedera.block.server.metrics.BlockNodeMetricTypes;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.BlockVerificationStatus;
import com.hedera.block.server.verification.VerificationResult;
import com.hedera.block.server.verification.hasher.CommonUtils;
import com.hedera.block.server.verification.hasher.Hashes;
import com.hedera.block.server.verification.hasher.StreamingTreeHasher;
import com.hedera.block.server.verification.signature.SignatureVerifier;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract base class providing common functionality for block verification sessions.
 * Concrete classes handle how block items are appended and processed (synchronously or asynchronously).
 */
public abstract class AbstractBlockVerificationSession implements BlockVerificationSession {

    protected final System.Logger LOGGER = System.getLogger(getClass().getName());

    protected final MetricsService metricsService;
    protected final SignatureVerifier signatureVerifier;
    protected final long blockNumber;
    protected final StreamingTreeHasher inputTreeHasher;
    protected final StreamingTreeHasher outputTreeHasher;
    protected final long blockWorkStartTime;

    protected volatile boolean running = true;
    protected final CompletableFuture<VerificationResult> verificationResultFuture = new CompletableFuture<>();

    /**
     * Constructs the session with shared initialization logic.
     *
     * @param blockHeader the block header
     * @param metricsService the metrics service
     * @param signatureVerifier the signature verifier
     * @param inputTreeHasher the input tree hasher (e.g. naive or concurrent)
     * @param outputTreeHasher the output tree hasher (e.g. naive or concurrent)
     */
    protected AbstractBlockVerificationSession(
            @NonNull final BlockHeader blockHeader,
            @NonNull final MetricsService metricsService,
            @NonNull final SignatureVerifier signatureVerifier,
            @NonNull final StreamingTreeHasher inputTreeHasher,
            @NonNull final StreamingTreeHasher outputTreeHasher) {
        this.blockNumber = blockHeader.number();
        this.metricsService = metricsService;
        this.signatureVerifier = signatureVerifier;
        this.inputTreeHasher = inputTreeHasher;
        this.outputTreeHasher = outputTreeHasher;

        this.blockWorkStartTime = System.nanoTime();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public CompletableFuture<VerificationResult> getVerificationResult() {
        return verificationResultFuture;
    }

    /**
     * Processes the provided block items by updating the tree hashers.
     * If the last item has a block proof, final verification is triggered.
     *
     * @param blockItems the block items to process
     * @throws ParseException if a parsing error occurs
     */
    protected void processBlockItems(List<BlockItemUnparsed> blockItems) throws ParseException {

        Hashes hashes = CommonUtils.getBlockHashes(blockItems);
        while (hashes.inputHashes().hasRemaining()) {
            inputTreeHasher.addLeaf(hashes.inputHashes());
        }
        while (hashes.outputHashes().hasRemaining()) {
            outputTreeHasher.addLeaf(hashes.outputHashes());
        }

        // Check if this batch contains the final block proof
        final BlockItemUnparsed lastItem = blockItems.getLast();
        if (lastItem.hasBlockProof()) {
            BlockProof blockProof = BlockProof.PROTOBUF.parse(lastItem.blockProof());
            finalizeVerification(blockProof);
        }
    }

    /**
     * Finalizes the block verification by computing the final block hash,
     * verifying its signature, and updating metrics accordingly.
     *
     * @param blockProof the block proof
     */
    protected void finalizeVerification(BlockProof blockProof) {
        Bytes blockHash = CommonUtils.computeFinalBlockHash(blockProof, inputTreeHasher, outputTreeHasher);
        VerificationResult result;
        boolean verified = signatureVerifier.verifySignature(blockHash, blockProof.blockSignature());
        if (verified) {
            long verificationLatency = System.nanoTime() - blockWorkStartTime;
            metricsService
                    .get(BlockNodeMetricTypes.Counter.VerificationBlockTime)
                    .add(verificationLatency);
            metricsService
                    .get(BlockNodeMetricTypes.Counter.VerificationBlocksVerified)
                    .increment();

            result = new VerificationResult(blockNumber, blockHash, BlockVerificationStatus.VERIFIED);
        } else {
            LOGGER.log(INFO, "Block verification failed for block number: {0}", blockNumber);
            metricsService
                    .get(BlockNodeMetricTypes.Counter.VerificationBlocksFailed)
                    .increment();

            result = new VerificationResult(blockNumber, blockHash, BlockVerificationStatus.SIGNATURE_INVALID);
        }
        shutdownSession();
        verificationResultFuture.complete(result);
    }

    /**
     * Shuts down this session, marking it as no longer running.
     */
    protected void shutdownSession() {
        running = false;
    }

    /**
     * A helper method that handles errors encountered during block item processing.
     *
     * @param ex the exception encountered
     */
    protected void handleProcessingError(Throwable ex) {
        LOGGER.log(System.Logger.Level.ERROR, "Error processing block items", ex);
        metricsService.get(BlockNodeMetricTypes.Counter.VerificationBlocksError).increment();
        verificationResultFuture.completeExceptionally(ex);
    }
}
