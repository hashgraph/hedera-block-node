// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.hedera.block.server.ack.AckHandlerImpl;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private AckHandlerImpl toTest;

    @BeforeEach
    void setUp() {
        // By default, we do NOT skip acknowledgements
        Counter metric = mock(Counter.class);
        lenient()
                .when(metricsService.get(BlockNodeMetricTypes.Counter.AckedBlocked))
                .thenReturn(metric);
        toTest = new AckHandlerImpl(notifier, false, serviceStatus, blockRemover, metricsService);
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
        toTest.blockVerificationFailed(2L);

        // then
        verify(notifier, times(1)).sendEndOfStream(-1L, PublishStreamResponseCode.STREAM_ITEMS_BAD_STATE_PROOF);
        verifyNoMoreInteractions(notifier);
    }

    @Test
    @DisplayName("blockPersisted alone does not ACK")
    void blockPersisted_thenNoAckWithoutVerification() {
        // when
        final long blockNumber = 1L;
        toTest.blockPersisted(new BlockPersistenceResult(blockNumber, BlockPersistenceStatus.SUCCESS));

        // then
        // We have not verified the block, so no ACK is sent
        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("blockVerified alone does not ACK")
    void blockVerified_thenNoAckWithoutPersistence() {
        // when
        toTest.blockVerified(1L, Bytes.wrap("hash1".getBytes()));

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
        toTest.blockPersisted(new BlockPersistenceResult(blockNumber, BlockPersistenceStatus.SUCCESS));
        toTest.blockVerified(blockNumber, blockHash);

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
        toTest.blockPersisted(new BlockPersistenceResult(block1, BlockPersistenceStatus.SUCCESS));
        toTest.blockVerified(block1, hash1);

        // Mark block2 persisted and verified
        toTest.blockPersisted(new BlockPersistenceResult(block2, BlockPersistenceStatus.SUCCESS));
        toTest.blockVerified(block2, hash2);

        // Mark block3 persisted and verified
        toTest.blockPersisted(new BlockPersistenceResult(block3, BlockPersistenceStatus.SUCCESS));
        toTest.blockVerified(block3, hash3);

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
        final long block1 = 10L;
        final long block2 = 11L;
        final Bytes hash1 = Bytes.wrap("hash10".getBytes());
        final Bytes hash2 = Bytes.wrap("hash11".getBytes());

        // when
        // Fully persist & verify block #10 -> Should ACK
        toTest.blockPersisted(new BlockPersistenceResult(block1, BlockPersistenceStatus.SUCCESS));
        toTest.blockVerified(block1, hash1);

        // Partially persist block #11
        toTest.blockPersisted(new BlockPersistenceResult(block2, BlockPersistenceStatus.SUCCESS));
        // We do NOT verify block #11 yet

        // then
        // Should only ACK block #10
        verify(notifier, times(1)).sendAck(eq(block1), eq(hash1), eq(false));
        verifyNoMoreInteractions(notifier);

        // Now verify block #11
        toTest.blockVerified(block2, hash2);

        // Expect the second ACK
        verify(notifier, times(1)).sendAck(eq(block2), eq(hash2), eq(false));
        verifyNoMoreInteractions(notifier);
    }
}
