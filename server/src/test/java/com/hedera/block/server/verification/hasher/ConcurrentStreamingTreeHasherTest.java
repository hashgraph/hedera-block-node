// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.verification.hasher;

import static com.hedera.block.server.verification.hasher.ConcurrentStreamingTreeHasher.rootHashFrom;
import static com.hedera.block.server.verification.hasher.StreamingTreeHasher.HASH_LENGTH;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.nio.ByteBuffer;
import java.util.SplittableRandom;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConcurrentStreamingTreeHasherTest {
    private static final SplittableRandom RANDOM = new SplittableRandom();

    private final NaiveStreamingTreeHasher comparison = new NaiveStreamingTreeHasher();
    private final ConcurrentStreamingTreeHasher subject = new ConcurrentStreamingTreeHasher(ForkJoinPool.commonPool());

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 5, 32, 69, 100, 123, 234})
    void testAddLeafAndRootHash(final int numLeaves) {
        ByteBuffer lastLeafHash = null;
        var status = StreamingTreeHasher.Status.EMPTY;
        for (int i = 1; i <= numLeaves; i++) {
            final var hash = new byte[HASH_LENGTH];
            RANDOM.nextBytes(hash);
            final var leafHash = ByteBuffer.wrap(hash);
            subject.addLeaf(ByteBuffer.wrap(hash));
            comparison.addLeaf(ByteBuffer.wrap(hash));
            if (i == numLeaves - 1) {
                status = subject.status();
            } else if (i == numLeaves) {
                lastLeafHash = leafHash;
            }
        }

        final var actual = subject.rootHash().join();
        final var expected = comparison.rootHash().join();
        assertEquals(expected, actual);
        if (lastLeafHash != null) {
            requireNonNull(status);
            final var recalculated = rootHashFrom(status, Bytes.wrap(lastLeafHash.array()));
            assertEquals(expected, recalculated);
        }
    }

    @Test
    void testAddLeafAfterRootHashRequested() {
        final var leaf = ByteBuffer.allocate(48);
        subject.addLeaf(leaf);
        subject.rootHash();
        assertThrows(IllegalStateException.class, () -> subject.addLeaf(leaf));
    }
}
