// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.producer;

import com.hedera.hapi.block.BlockItemUnparsed;
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
    public static byte[] getFakeHash(@NonNull final List<BlockItemUnparsed> blockItems)
            throws NoSuchAlgorithmException {
        // Calculate the SHA-384 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-384");
        return digest.digest(blockItems.getLast().blockProof().toByteArray());
    }
}
