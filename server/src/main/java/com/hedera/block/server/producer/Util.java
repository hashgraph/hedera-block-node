// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.producer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Utility class for the BlockNode service. */
public final class Util {
    private Util() {}

    public static byte[] getFakeHash() throws NoSuchAlgorithmException {
        // Calculate the SHA-384 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-384");
        return digest.digest("fake_hash".getBytes());
    }
}
