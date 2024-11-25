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
