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

package com.hedera.block.server.verification;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import com.hedera.block.server.metrics.BlockNodeMetricTypes;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.verification.service.BlockVerificationService;
import com.hedera.block.server.verification.service.BlockVerificationServiceImpl;
import com.hedera.block.server.verification.session.BlockVerificationSession;
import com.hedera.block.server.verification.session.BlockVerificationSessionFactory;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.swirlds.metrics.api.Counter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BlockVerificationServiceImplTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private BlockVerificationSessionFactory sessionFactory;

    @Mock
    private BlockVerificationSession previousSession;

    @Mock
    private BlockVerificationSession newSession;

    @Mock
    private Counter verificationBlocksReceived;

    @Mock
    private Counter verificationBlocksFailed;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(metricsService.get(BlockNodeMetricTypes.Counter.VerificationBlocksReceived))
                .thenReturn(verificationBlocksReceived);
        when(metricsService.get(BlockNodeMetricTypes.Counter.VerificationBlocksFailed))
                .thenReturn(verificationBlocksFailed);
    }

    @Test
    void testOnBlockItemsReceivedWithNewBlockHeaderNoPreviousSession() throws ParseException {
        // Given a new block header starting at block #10
        long blockNumber = 10;
        BlockItemUnparsed blockHeaderItem = getBlockHeaderUnparsed(blockNumber);
        List<BlockItemUnparsed> blockItems = List.of(blockHeaderItem);

        // No previous session
        when(sessionFactory.createSession(any())).thenReturn(newSession);

        BlockVerificationService service = new BlockVerificationServiceImpl(metricsService, sessionFactory);

        // When
        service.onBlockItemsReceived(blockItems);

        // Then
        verify(verificationBlocksReceived).increment(); // new block received
        verify(sessionFactory).createSession(getBlockHeader(blockNumber));
        verify(newSession).appendBlockItems(blockItems);
        // No previous session, so just logs a warning internally
    }

    @Test
    void testOnBlockItemsReceivedWithNewBlockHeaderAndPreviousSessionHashMatch() throws ParseException {
        // Given a previous verified block #9 and now receiving header for block #10
        long previousBlockNumber = 9;
        long newBlockNumber = 10;

        BlockItemUnparsed blockHeaderItem = getBlockHeaderUnparsed(newBlockNumber);
        List<BlockItemUnparsed> blockItems = List.of(blockHeaderItem);

        // Previous session result matches the expected previous hash
        CompletableFuture<VerificationResult> future = new CompletableFuture<>();
        future.complete(getVerificationResult(previousBlockNumber));
        when(previousSession.getVerificationResult()).thenReturn(future);

        when(sessionFactory.createSession(getBlockHeader(newBlockNumber))).thenReturn(newSession);

        BlockVerificationServiceImpl service = new BlockVerificationServiceImpl(metricsService, sessionFactory);
        setCurrentSession(service, previousSession);

        // When
        service.onBlockItemsReceived(blockItems);

        // Then
        verify(verificationBlocksReceived).increment();
        verify(verificationBlocksFailed, never()).increment();
        verify(newSession).appendBlockItems(blockItems);
    }

    @Test
    void testOnBlockItemsReceivedWithNewBlockHeaderAndPreviousSessionHashMismatch() throws ParseException {
        // Given a previous block #9 but now we produce a verification result that doesn't match the new header's prev
        // hash
        long previousBlockNumber = 9;
        long newBlockNumber = 10;

        BlockItemUnparsed blockHeaderItem = getBlockHeaderUnparsed(newBlockNumber);
        List<BlockItemUnparsed> blockItems = List.of(blockHeaderItem);

        // Make the previous session result have a different hash (e.g., block #99)
        CompletableFuture<VerificationResult> future = new CompletableFuture<>();
        future.complete(getVerificationResult(99)); // This gives hash99, not hash9
        when(previousSession.getVerificationResult()).thenReturn(future);

        when(sessionFactory.createSession(getBlockHeader(newBlockNumber))).thenReturn(newSession);

        BlockVerificationServiceImpl service = new BlockVerificationServiceImpl(metricsService, sessionFactory);
        setCurrentSession(service, previousSession);

        // When
        service.onBlockItemsReceived(blockItems);

        // Then
        verify(verificationBlocksReceived).increment();
        verify(verificationBlocksFailed).increment(); // mismatch should cause increment
        verify(newSession).appendBlockItems(blockItems);
    }

    @Test
    void testOnBlockItemsReceivedNoBlockHeaderNoCurrentSession() throws ParseException {
        BlockItemUnparsed normalItem = getNormalBlockItem();
        List<BlockItemUnparsed> blockItems = List.of(normalItem);

        BlockVerificationService service = new BlockVerificationServiceImpl(metricsService, sessionFactory);

        // When
        service.onBlockItemsReceived(blockItems);

        // Then
        // Just logs a warning. No increments or sessions created.
        verifyNoInteractions(sessionFactory);
        verifyNoInteractions(verificationBlocksReceived, verificationBlocksFailed);
    }

    @Test
    void testOnBlockItemsReceivedNoBlockHeaderWithCurrentSession() throws ParseException {
        BlockItemUnparsed normalItem = getNormalBlockItem();
        List<BlockItemUnparsed> blockItems = List.of(normalItem);

        BlockVerificationServiceImpl service = new BlockVerificationServiceImpl(metricsService, sessionFactory);
        setCurrentSession(service, previousSession);

        // When
        service.onBlockItemsReceived(blockItems);

        // Then
        verify(previousSession).appendBlockItems(blockItems);
        verifyNoInteractions(verificationBlocksReceived, verificationBlocksFailed);
    }

    private VerificationResult getVerificationResult(long blockNumber) {
        return new VerificationResult(
                blockNumber, Bytes.wrap(("hash" + blockNumber).getBytes()), BlockVerificationStatus.VERIFIED);
    }

    private BlockHeader getBlockHeader(long blockNumber) {
        long previousBlockNumber = blockNumber - 1;

        return BlockHeader.newBuilder()
                .previousBlockHash(Bytes.wrap(("hash" + previousBlockNumber).getBytes()))
                .number(blockNumber)
                .build();
    }

    private BlockItemUnparsed getBlockHeaderUnparsed(long blockNumber) {
        return BlockItemUnparsed.newBuilder()
                .blockHeader(BlockHeader.PROTOBUF.toBytes(getBlockHeader(blockNumber)))
                .build();
    }

    private BlockItemUnparsed getNormalBlockItem() {
        // A block item without a block header
        return BlockItemUnparsed.newBuilder().build();
    }

    // Helper method to set the currentSession field via reflection since it’s private
    private static void setCurrentSession(BlockVerificationServiceImpl service, BlockVerificationSession session) {
        try {
            var field = BlockVerificationServiceImpl.class.getDeclaredField("currentSession");
            field.setAccessible(true);
            field.set(service, session);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to set currentSession via reflection", e);
        }
    }
}
