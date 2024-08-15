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

import static com.hedera.block.server.producer.Util.getFakeHash;

import com.hedera.hapi.block.Acknowledgement;
import com.hedera.hapi.block.ItemAcknowledgement;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.security.NoSuchAlgorithmException;

/**
 * The ItemAckBuilder class defines a simple item acknowledgement builder used to create an
 * acknowledgement type response. This is a placeholder and should be replaced with real hash
 * functionality once the hedera-protobufs types are integrated.
 */
public class AckBuilder {

    /** Constructor for the ItemAckBuilder class. */
    public AckBuilder() {}

    @NonNull
    public Acknowledgement buildAck(@NonNull final BlockItem blockItem)
            throws NoSuchAlgorithmException {
        ItemAcknowledgement itemAck =
                ItemAcknowledgement.newBuilder()
                        .itemHash(Bytes.wrap(getFakeHash(blockItem)))
                        .build();
        return Acknowledgement.newBuilder().itemAck(itemAck).build();
    }
}
