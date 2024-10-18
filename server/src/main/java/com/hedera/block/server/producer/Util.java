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

import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/** Utility class for the BlockNode service. */
public final class Util {
    private Util() {}

    /**
     * Gets a fake hash for the given block item. This is a placeholder and should be replaced with
     * real hash functionality once the hedera-protobufs types are integrated.
     *
     * @param blockItems the block item to get the fake hash for
     * @return the fake hash for the given block item
     * @throws NoSuchAlgorithmException thrown if the SHA-384 algorithm is not available
     */
    public static byte[] getFakeHash(@NonNull final List<BlockItem> blockItems)
            throws NoSuchAlgorithmException {
        // Calculate the SHA-384 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-384");
        return digest.digest(BlockItem.PROTOBUF.toBytes(blockItems.getLast()).toByteArray());
    }
}
