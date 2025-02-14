// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.hedera.block.server.ack.AckHandlerImpl;
import com.hedera.block.server.block.BlockInfo;
import com.hedera.block.server.metrics.BlockNodeMetricTypes;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.persistence.storage.write.BlockPersistenceResult;
import com.hedera.block.server.persistence.storage.write.BlockPersistenceResult.BlockPersistenceStatus;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.swirlds.metrics.api.Counter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AckHandlerImplTest {

    @Mock
    private Notifier notifier;

    @Mock
    private ServiceStatus serviceStatus;

    @Mock
    private BlockRemover blockRemover;

    @Mock
    private MetricsService metricsService;

    private AckHandlerImpl ackHandler;

    @BeforeEach
    void setUp() {
        // By default, we do NOT skip acknowledgements
        Counter metric = mock(Counter.class);
        lenient()
                .when(metricsService.get(BlockNodeMetricTypes.Counter.AckedBlocked))
                .thenReturn(metric);
        ackHandler = new AckHandlerImpl(notifier, false, serviceStatus, blockRemover, metricsService);
    }

    @Test
    @DisplayName("blockVerified + blockPersisted should do nothing if skipAcknowledgement == true")
    void blockVerified_skippedAcknowledgement() {
        // given
        final AckHandlerImpl managerWithSkip =
                new AckHandlerImpl(notifier, true, serviceStatus, blockRemover, metricsService);

        // when
        final long blockNumber = 1L;
        managerWithSkip.blockVerified(blockNumber, Bytes.wrap("somehash".getBytes()));
        managerWithSkip.blockPersisted(new BlockPersistenceResult(blockNumber, BlockPersistenceStatus.SUCCESS));

        // then
        // No interactions with notifier should happen
        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("blockVerificationFailed should send end-of-stream message with appropriate code")
    void blockVerificationFailed_sendsEndOfStream() {
        // when
        ackHandler.blockVerificationFailed(2L);

        // then
        verify(notifier, times(1)).sendEndOfStream(-1L, PublishStreamResponseCode.STREAM_ITEMS_BAD_STATE_PROOF);
        verifyNoMoreInteractions(notifier);
    }

    @Test
    @DisplayName("blockPersisted alone does not ACK")
    void blockPersisted_thenNoAckWithoutVerification() {
        // when
        final long blockNumber = 1L;
        ackHandler.blockPersisted(new BlockPersistenceResult(blockNumber, BlockPersistenceStatus.SUCCESS));

        // then
        // We have not verified the block, so no ACK is sent
        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("blockVerified alone does not ACK")
    void blockVerified_thenNoAckWithoutPersistence() {
        // when
        ackHandler.blockVerified(1L, Bytes.wrap("hash1".getBytes()));

        // then
        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("blockPersisted + blockVerified triggers a single ACK")
    void blockPersistedThenBlockVerified_triggersAck() {
        // given
        final long blockNumber = 1L;
        final Bytes blockHash = Bytes.wrap("hash1".getBytes());

        // when
        ackHandler.blockPersisted(new BlockPersistenceResult(blockNumber, BlockPersistenceStatus.SUCCESS));
        ackHandler.blockVerified(blockNumber, blockHash);

        // then
        // We expect a single ACK for block #1
        verify(notifier, times(1)).sendAck(eq(blockNumber), eq(blockHash), eq(false));
        verifyNoMoreInteractions(notifier);
    }

    @Test
    @DisplayName(
            "Multiple consecutive blocks can be ACKed in sequence if they are both persisted and verified in order")
    void multipleBlocksAckInSequence() {
        // given
        final long block1 = 1L;
        final long block2 = 2L;
        final long block3 = 3L;
        final Bytes hash1 = Bytes.wrap("hash1".getBytes());
        final Bytes hash2 = Bytes.wrap("hash2".getBytes());
        final Bytes hash3 = Bytes.wrap("hash3".getBytes());

        // when
        // Mark block1 persisted and verified
        ackHandler.blockPersisted(new BlockPersistenceResult(block1, BlockPersistenceStatus.SUCCESS));
        ackHandler.blockVerified(block1, hash1);

        // Mark block2 persisted and verified
        ackHandler.blockPersisted(new BlockPersistenceResult(block2, BlockPersistenceStatus.SUCCESS));
        ackHandler.blockVerified(block2, hash2);

        // Mark block3 persisted and verified
        ackHandler.blockPersisted(new BlockPersistenceResult(block3, BlockPersistenceStatus.SUCCESS));
        ackHandler.blockVerified(block3, hash3);

        // then
        // The manager should ACK blocks in ascending order (1,2,3).
        final ArgumentCaptor<Long> blockNumberCaptor = ArgumentCaptor.forClass(Long.class);
        final ArgumentCaptor<Bytes> blockHashCaptor = ArgumentCaptor.forClass(Bytes.class);

        // We expect 3 calls to sendAck
        verify(notifier, times(3)).sendAck(blockNumberCaptor.capture(), blockHashCaptor.capture(), eq(false));

        final List<Long> capturedBlockNumbers = blockNumberCaptor.getAllValues();
        final List<Bytes> capturedHashes = blockHashCaptor.getAllValues();

        assertEquals(3, capturedBlockNumbers.size(), "We should have exactly 3 ACK calls");
        assertEquals(1L, capturedBlockNumbers.get(0));
        assertEquals(2L, capturedBlockNumbers.get(1));
        assertEquals(3L, capturedBlockNumbers.get(2));

        assertEquals(hash1, capturedHashes.get(0));
        assertEquals(hash2, capturedHashes.get(1));
        assertEquals(hash3, capturedHashes.get(2));

        verifyNoMoreInteractions(notifier);
    }

    @Test
    @DisplayName("Blocks are ACKed in order; partial readiness doesn't skip ahead")
    void ackStopsIfNextBlockIsNotReady() {
        // given
        final long block1 = 1L;
        final long block2 = 2L;
        final Bytes hash1 = Bytes.wrap("hash1".getBytes());
        final Bytes hash2 = Bytes.wrap("hash2".getBytes());

        // when
        // Fully persist & verify block #10 -> Should ACK
        ackHandler.blockPersisted(new BlockPersistenceResult(block1, BlockPersistenceStatus.SUCCESS));
        ackHandler.blockVerified(block1, hash1);

        // Partially persist block #11
        ackHandler.blockPersisted(new BlockPersistenceResult(block2, BlockPersistenceStatus.SUCCESS));
        // We do NOT verify block #11 yet

        // then
        // Should only ACK block #10
        verify(notifier, times(1)).sendAck(eq(block1), eq(hash1), eq(false));
        verifyNoMoreInteractions(notifier);

        // Now verify block #11
        ackHandler.blockVerified(block2, hash2);

        // Expect the second ACK
        verify(notifier, times(1)).sendAck(eq(block2), eq(hash2), eq(false));
        verifyNoMoreInteractions(notifier);
    }

    /**
     * Edge condition #1:
     * If only block 2 is processed (i.e. block 1 is missing)
     * then no ACK should be sent (because ACKs must be strictly consecutive).
     */
    @Test
    public void testAckNotSentWhenLowerBlockMissing() {
        final long block2 = 2L;
        final Bytes blockHash2 = Bytes.wrap("hash2".getBytes());

        // Simulate receiving persistence and verification for block 2.
        ackHandler.blockPersisted(new BlockPersistenceResult(block2, BlockPersistenceStatus.SUCCESS));
        ackHandler.blockVerified(block2, blockHash2);

        // In a correct implementation nothing should be ACKed because block 1 is missing.
        verify(notifier, never()).sendAck(eq(block2), any(), anyBoolean());
    }

    /**
     * Edge condition #2:
     * When block 2 is processed first (and ACKed) and then block 1 arrives,
     * the ACK order is wrong – block 1 should have been ACKed before block 2.
     */
    @Test
    public void testAckOrderWhenLowerBlockArrivesLate() {
        final long block2 = 2L;
        final Bytes blockHash2 = Bytes.wrap("hash2".getBytes());
        final long block1 = 1L;
        final Bytes blockHash1 = Bytes.wrap("hash1".getBytes());

        // First, process events for block 2.
        ackHandler.blockPersisted(new BlockPersistenceResult(block2, BlockPersistenceStatus.SUCCESS));
        ackHandler.blockVerified(block2, blockHash2);

        // Then, process events for block 1.
        ackHandler.blockPersisted(new BlockPersistenceResult(block1, BlockPersistenceStatus.SUCCESS));
        ackHandler.blockVerified(block1, blockHash1);

        // In a correct implementation the ACKs would be sent in order: first for block 1 then block 2.
        InOrder inOrder = inOrder(notifier);
        inOrder.verify(notifier).sendAck(eq(block1), eq(blockHash1), eq(false));
        inOrder.verify(notifier).sendAck(eq(block2), eq(blockHash2), eq(false));
    }

    @ParameterizedTest
    @CsvSource({
        // Format: blockCount, maxPersistDelayNanos, maxVerifyDelayNanos
        "100, 0, 0",
        "1000, 0, 0",
        "10000, 0, 0",
        "100, 1000, 0",
        "1000, 1000, 0",
        "10000, 1000, 0",
        "100, 0, 1000",
        "1000, 0, 1000",
        "10000, 0, 1000",
    })
    void highlyConcurrentAckHandlerTest(int blockCount, int maxPersistDelayNanos, int maxVerifyDelayNanos)
            throws Exception {
        // Create the instance under test (with skipAcknowledgement = false).
        AckHandlerImpl ackHandler = new AckHandlerImpl(notifier, false, serviceStatus, blockRemover, metricsService);

        // Use an ExecutorService to run two concurrent tasks.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        Random random = new Random();

        // Thread that sends persistence events.
        Runnable persistTask = () -> {
            try {
                startLatch.await();
                for (int i = 1; i <= blockCount; i++) {
                    ackHandler.blockPersisted(i);
                    if (maxPersistDelayNanos > 0) {
                        long delay = random.nextInt(maxPersistDelayNanos + 1);
                        TimeUnit.NANOSECONDS.sleep(delay);
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        // Thread that sends verification events.
        Runnable verifyTask = () -> {
            try {
                startLatch.await();
                for (int i = 1; i <= blockCount; i++) {
                    Bytes blockHash = bytesFromLong(i);
                    ackHandler.blockVerified(i, blockHash);
                    if (maxVerifyDelayNanos > 0) {
                        long delay = random.nextInt(maxVerifyDelayNanos + 1);
                        TimeUnit.NANOSECONDS.sleep(delay);
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(persistTask);
        executor.submit(verifyTask);
        startLatch.countDown();
        assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "Tasks did not complete in time");
        executor.shutdown();
        // Wait a bit to ensure all ACKs are processed.
        Thread.sleep(100);

        ArgumentCaptor<Long> blockNumberCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Bytes> blockHashCaptor = ArgumentCaptor.forClass(Bytes.class);
        verify(notifier, times(blockCount))
                .sendAck(blockNumberCaptor.capture(), blockHashCaptor.capture(), anyBoolean());
        List<Long> capturedBlockNumbers = blockNumberCaptor.getAllValues();
        List<Bytes> capturedBlockHashes = blockHashCaptor.getAllValues();

        assertEquals(blockCount, capturedBlockNumbers.size(), "Number of ACKs mismatch");
        for (int i = 0; i < blockCount; i++) {
            long expected = i + 1;
            assertEquals(expected, capturedBlockNumbers.get(i), "ACK order mismatch at index " + i);
            assertEquals(
                    bytesFromLong(expected), capturedBlockHashes.get(i), "Block hash mismatch at block " + expected);
        }
        // verify that the serviceStatus was updated with the final block.
        ArgumentCaptor<BlockInfo> blockInfoCaptor = ArgumentCaptor.forClass(BlockInfo.class);
        verify(serviceStatus, atLeastOnce()).setLatestAckedBlock(blockInfoCaptor.capture());
        BlockInfo latest = blockInfoCaptor.getValue();
        assertNotNull(latest, "Latest acked block should not be null");
        assertEquals(blockCount, latest.getBlockNumber(), "Latest acknowledged block number mismatch");
    }

    // Helper method to create a dummy Bytes object from a long.
    private static Bytes bytesFromLong(long value) {
        byte[] arr = new byte[8];
        for (int i = 7; i >= 0; i--) {
            arr[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return Bytes.wrap(arr);
    }
}
