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

package com.hedera.block.server.producer;

import static com.hedera.block.protos.BlockStreamService.BlockItem;
import static com.hedera.block.protos.BlockStreamService.PublishStreamResponse.ItemAcknowledgement;
import static com.hedera.block.server.producer.Util.getFakeHash;

import com.google.protobuf.ByteString;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * The ItemAckBuilder class defines a simple item acknowledgement builder used to create an
 * acknowledgement type response. This is a placeholder and should be replaced with real hash
 * functionality once the hedera-protobufs types are integrated.
 */
public class ItemAckBuilder {

    /** Constructor for the ItemAckBuilder class. */
    public ItemAckBuilder() {}

    /**
     * Builds an item acknowledgement for the given block item.
     *
     * @param blockItem the block item to build the acknowledgement for
     * @return the item acknowledgement for the given block item
     * @throws IOException thrown if an I/O error occurs while building the acknowledgement
     * @throws NoSuchAlgorithmException thrown if the SHA-384 algorithm is not available
     */
    @NonNull
    public ItemAcknowledgement buildAck(@NonNull final BlockItem blockItem)
            throws IOException, NoSuchAlgorithmException {
        // TODO: Use real hash and real hedera-protobufs types
        return ItemAcknowledgement.newBuilder()
                .setItemAck(ByteString.copyFrom(getFakeHash(blockItem)))
                .build();
    }
}
