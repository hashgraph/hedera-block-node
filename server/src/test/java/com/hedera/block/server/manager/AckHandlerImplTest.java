// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.hedera.block.server.ack.AckHandlerImpl;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.remove.BlockRemover;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.PublishStreamResponseCode;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AckHandlerImplTest {

    private Notifier notifier;
    private AckHandlerImpl ackHandler;

    @Mock
    private ServiceStatus serviceStatus;

    @Mock
    private BlockRemover blockRemover;

    @Mock
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        // By default, we do NOT skip acknowledgements
        ackHandler = new AckHandlerImpl(notifier, false, serviceStatus, blockRemover, metricsService);
    }

    @Test
    @DisplayName("blockVerified + blockPersisted should do nothing if skipAcknowledgement == true")
    void blockVerified_skippedAcknowledgement() {
        // given
        AckHandlerImpl managerWithSkip = new AckHandlerImpl(notifier, true, serviceStatus, blockRemover, metricsService);

        // when
        managerWithSkip.blockVerified(1L, Bytes.wrap("somehash".getBytes()));
        managerWithSkip.blockPersisted(1L);

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
        ackHandler.blockPersisted(1L);

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
        long blockNumber = 1L;
        Bytes blockHash = Bytes.wrap("hash1".getBytes());

        // when
        ackHandler.blockPersisted(blockNumber);
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
        long block1 = 1L;
        long block2 = 2L;
        long block3 = 3L;
        Bytes hash1 = Bytes.wrap("hash1".getBytes());
        Bytes hash2 = Bytes.wrap("hash2".getBytes());
        Bytes hash3 = Bytes.wrap("hash3".getBytes());

        // when
        // Mark block1 persisted and verified
        ackHandler.blockPersisted(block1);
        ackHandler.blockVerified(block1, hash1);

        // Mark block2 persisted and verified
        ackHandler.blockPersisted(block2);
        ackHandler.blockVerified(block2, hash2);

        // Mark block3 persisted and verified
        ackHandler.blockPersisted(block3);
        ackHandler.blockVerified(block3, hash3);

        // then
        // The manager should ACK blocks in ascending order (1,2,3).
        ArgumentCaptor<Long> blockNumberCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Bytes> blockHashCaptor = ArgumentCaptor.forClass(Bytes.class);

        // We expect 3 calls to sendAck
        verify(notifier, times(3)).sendAck(blockNumberCaptor.capture(), blockHashCaptor.capture(), eq(false));

        List<Long> capturedBlockNumbers = blockNumberCaptor.getAllValues();
        List<Bytes> capturedHashes = blockHashCaptor.getAllValues();

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
        long block1 = 10L;
        long block2 = 11L;
        Bytes hash1 = Bytes.wrap("hash10".getBytes());
        Bytes hash2 = Bytes.wrap("hash11".getBytes());

        // when
        // Fully persist & verify block #10 -> Should ACK
        ackHandler.blockPersisted(block1);
        ackHandler.blockVerified(block1, hash1);

        // Partially persist block #11
        ackHandler.blockPersisted(block2);
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
}
