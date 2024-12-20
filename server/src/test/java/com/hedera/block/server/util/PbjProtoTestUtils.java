// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.util;

import static com.hedera.block.server.producer.Util.getFakeHash;

import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.BlockItemSetUnparsed;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.ItemAcknowledgement;
import com.hedera.hapi.block.PublishStreamRequestUnparsed;
import com.hedera.hapi.block.SubscribeStreamRequest;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public final class PbjProtoTestUtils {
    private PbjProtoTestUtils() {}

    public static Acknowledgement buildAck(@NonNull final List<BlockItemUnparsed> blockItems)
            throws NoSuchAlgorithmException {
        ItemAcknowledgement itemAck = ItemAcknowledgement.newBuilder()
                .itemsHash(Bytes.wrap(getFakeHash(blockItems)))
                .build();

        return Acknowledgement.newBuilder().itemAck(itemAck).build();
    }

    public static Bytes buildEmptyPublishStreamRequest() {
        return PublishStreamRequestUnparsed.PROTOBUF.toBytes(PublishStreamRequestUnparsed.newBuilder()
                .blockItems(BlockItemSetUnparsed.newBuilder().build())
                .build());
    }

    public static Bytes buildEmptySubscribeStreamRequest() {
        return SubscribeStreamRequest.PROTOBUF.toBytes(
                SubscribeStreamRequest.newBuilder().build());
    }
}
