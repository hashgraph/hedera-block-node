// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.write;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link NoOpBlockWriter}.
 */
class NoOpBlockWriterTest {
    private NoOpBlockWriter toTest;

    @BeforeEach
    void setUp() {
        toTest = NoOpBlockWriter.newInstance();
    }

    /**
     * This test aims to verify that the {@link NoOpBlockWriter#write(List)}
     * does nothing and does not throw any exceptions. The no-op writer has no
     * preconditions check as well. The method will return the input list of
     * block items unparsed if the list ends with a block proof.
     */
    @Test
    void testSuccessfulBlockWrite() throws IOException, ParseException {
        final BlockProof blockProof = BlockProof.newBuilder().build();
        final Bytes blockProofAsBytes = BlockProof.PROTOBUF.toBytes(blockProof);
        final BlockItemUnparsed blockProofUnparsed =
                BlockItemUnparsed.newBuilder().blockProof(blockProofAsBytes).build();

        final BlockHeader blockHeader = BlockHeader.newBuilder().number(1L).build();
        final Bytes blockHeaderAsBytes = BlockHeader.PROTOBUF.toBytes(blockHeader);
        final BlockItemUnparsed blockHeaderUnparsed =
                BlockItemUnparsed.newBuilder().blockHeader(blockHeaderAsBytes).build();

        final List<BlockItemUnparsed> itemsToWrite = List.of(blockHeaderUnparsed, blockProofUnparsed);

        final Optional<Long> actual = toTest.write(itemsToWrite);
        assertThat(actual).isNotNull().isNotEmpty().containsSame(1L);
    }

    /**
     * This test aims to verify that the {@link NoOpBlockWriter#write(List)}
     * does nothing and does not throw any exceptions. The no-op writer has no
     * preconditions check as well. The method will return an empty optional if
     * the list does not end with a block proof.
     */
    @Test
    void testSuccessfulBlockWriteNoProof() throws IOException, ParseException {
        final BlockHeader blockHeader = BlockHeader.newBuilder().number(1L).build();
        final Bytes blockHeaderAsBytes = BlockHeader.PROTOBUF.toBytes(blockHeader);
        final BlockItemUnparsed blockHeaderUnparsed =
                BlockItemUnparsed.newBuilder().blockHeader(blockHeaderAsBytes).build();
        final List<BlockItemUnparsed> itemsToWrite = List.of(blockHeaderUnparsed);

        final Optional<Long> actual = toTest.write(itemsToWrite);
        assertThat(actual).isNotNull().isEmpty();
    }
}
