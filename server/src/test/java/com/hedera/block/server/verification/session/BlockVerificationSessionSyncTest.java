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

import static com.hedera.block.common.utils.FileUtilities.readGzipFileUnsafe;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.VerificationBlockTime;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.VerificationBlocksError;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.VerificationBlocksFailed;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.VerificationBlocksVerified;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.BlockVerificationStatus;
import com.hedera.block.server.verification.VerificationResult;
import com.hedera.block.server.verification.signature.SignatureVerifier;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.swirlds.metrics.api.Counter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BlockVerificationSessionSyncTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private SignatureVerifier signatureVerifier;

    @Mock
    private Counter verificationBlocksVerified;

    @Mock
    private Counter verificationBlocksFailed;

    @Mock
    private Counter verificationBlockTime;

    @Mock
    Counter verificationBlocksError;

    final Bytes hashing01BlockHash = Bytes.fromHex(
            "006ae77f87ff57df598f4d6536dcb5c0a5c1f840c2fef817b2faebd554d32cfc9a4eaee1d873ed88de668b53b7839117");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // set metrics
        when(metricsService.get(VerificationBlocksVerified)).thenReturn(verificationBlocksVerified);
        when(metricsService.get(VerificationBlocksFailed)).thenReturn(verificationBlocksFailed);
        when(metricsService.get(VerificationBlockTime)).thenReturn(verificationBlockTime);
        when(metricsService.get(VerificationBlocksError)).thenReturn(verificationBlocksError);
    }

    private List<BlockItemUnparsed> getTestBlock1Items() throws IOException, ParseException, URISyntaxException {
        Path block01Path =
                Path.of(getClass().getResource("/test-blocks/hashing-01.blk.gz").toURI());
        Bytes block01Bytes = Bytes.wrap(readGzipFileUnsafe(block01Path));
        BlockUnparsed blockUnparsed = BlockUnparsed.PROTOBUF.parse(block01Bytes);

        return blockUnparsed.blockItems();
    }

    @Test
    void testSuccessfulVerification() throws Exception {
        // Given
        List<BlockItemUnparsed> blockItems = getTestBlock1Items();
        BlockHeader blockHeader =
                BlockHeader.PROTOBUF.parse(blockItems.getFirst().blockHeader());
        BlockVerificationSessionSync session =
                new BlockVerificationSessionSync(blockHeader, metricsService, signatureVerifier);
        when(signatureVerifier.verifySignature(any(Bytes.class), any(Bytes.class)))
                .thenReturn(true);

        // When
        session.appendBlockItems(blockItems);
        CompletableFuture<VerificationResult> future = session.getVerificationResult();
        VerificationResult result = future.get();

        // Then
        assertEquals(BlockVerificationStatus.VERIFIED, result.status());
        assertEquals(1L, result.blockNumber());
        assertEquals(hashing01BlockHash, result.blockHash());
        assertFalse(session.isRunning());
        verify(verificationBlocksVerified, times(1)).increment();
        verify(verificationBlockTime, times(1)).add(any(Long.class));
        verifyNoMoreInteractions(verificationBlocksFailed);
    }

    @Test
    void testSuccessfulVerification_multipleAppends() throws Exception {
        // Given
        List<BlockItemUnparsed> blockItems = getTestBlock1Items();
        // Slice list into 2 parts of different sizes
        List<BlockItemUnparsed> blockItems1 = blockItems.subList(0, 3);
        List<BlockItemUnparsed> blockItems2 = blockItems.subList(3, blockItems.size());
        BlockHeader blockHeader =
                BlockHeader.PROTOBUF.parse(blockItems.getFirst().blockHeader());
        BlockVerificationSessionSync session =
                new BlockVerificationSessionSync(blockHeader, metricsService, signatureVerifier);
        when(signatureVerifier.verifySignature(any(Bytes.class), any(Bytes.class)))
                .thenReturn(true);

        // When
        session.appendBlockItems(blockItems1);
        session.appendBlockItems(blockItems2);
        CompletableFuture<VerificationResult> future = session.getVerificationResult();
        VerificationResult result = future.get();

        // Then
        assertEquals(BlockVerificationStatus.VERIFIED, result.status());
        assertEquals(1L, result.blockNumber());
        assertEquals(hashing01BlockHash, result.blockHash());
        assertFalse(session.isRunning());
        verify(verificationBlocksVerified, times(1)).increment();
        verify(verificationBlockTime, times(1)).add(any(Long.class));
        verifyNoMoreInteractions(verificationBlocksFailed);
    }

    @Test
    void testVerificationFailure() throws Exception {
        // Given
        List<BlockItemUnparsed> blockItems = getTestBlock1Items();
        final Bytes hashing01BlockHash = Bytes.fromHex(
                "006ae77f87ff57df598f4d6536dcb5c0a5c1f840c2fef817b2faebd554d32cfc9a4eaee1d873ed88de668b53b7839117");
        BlockHeader blockHeader =
                BlockHeader.PROTOBUF.parse(blockItems.getFirst().blockHeader());
        BlockVerificationSessionSync session =
                new BlockVerificationSessionSync(blockHeader, metricsService, signatureVerifier);
        when(signatureVerifier.verifySignature(any(Bytes.class), any(Bytes.class)))
                .thenReturn(false);

        // When
        session.appendBlockItems(blockItems);
        CompletableFuture<VerificationResult> future = session.getVerificationResult();
        VerificationResult result = future.get();

        // Then
        assertEquals(BlockVerificationStatus.SIGNATURE_INVALID, result.status());
        assertEquals(1L, result.blockNumber());
        assertEquals(hashing01BlockHash, result.blockHash());
        assertFalse(session.isRunning());
        verifyNoMoreInteractions(verificationBlocksVerified);
        verify(verificationBlocksFailed, times(1)).increment();
    }

    @Test
    void testAppendBlockItemsNotRunning() throws Exception {
        // Given
        List<BlockItemUnparsed> blockItems = getTestBlock1Items();
        BlockHeader blockHeader =
                BlockHeader.PROTOBUF.parse(blockItems.getFirst().blockHeader());
        BlockVerificationSessionSync session =
                new BlockVerificationSessionSync(blockHeader, metricsService, signatureVerifier);
        when(signatureVerifier.verifySignature(any(Bytes.class), any(Bytes.class)))
                .thenReturn(true);
        // send a whole block and wait for the result, the session should be completed.
        session.appendBlockItems(blockItems);
        CompletableFuture<VerificationResult> future = session.getVerificationResult();
        VerificationResult result = future.get();

        // metrics should be 1
        verify(verificationBlocksVerified, times(1)).increment();
        verify(verificationBlockTime, times(1)).add(any(Long.class));

        // When
        // Try to append more items after the session has completed
        session.appendBlockItems(blockItems);

        // Then
        // counters should still be 1
        verify(verificationBlocksVerified, times(1)).increment();
        verify(verificationBlockTime, times(1)).add(any(Long.class));
    }

    @Test
    void testParseException()
            throws IOException, ParseException, URISyntaxException, ExecutionException, InterruptedException {
        // Given
        List<BlockItemUnparsed> blockItems = getTestBlock1Items();
        BlockHeader blockHeader =
                BlockHeader.PROTOBUF.parse(blockItems.getFirst().blockHeader());
        blockItems.set(
                blockItems.size() - 1,
                BlockItemUnparsed.newBuilder().blockProof(Bytes.wrap("invalid")).build());
        BlockVerificationSessionSync session =
                new BlockVerificationSessionSync(blockHeader, metricsService, signatureVerifier);

        // When
        session.appendBlockItems(blockItems);
        CompletableFuture<VerificationResult> future = session.getVerificationResult();

        // Then
        assertTrue(future.isCompletedExceptionally());
        verify(verificationBlocksError, times(1)).increment();
    }
}
