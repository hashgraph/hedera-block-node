// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.verification.signature;

import org.junit.jupiter.api.Test;

class SignatureVerifierDummyTest {

    @Test
    void testVerifySignature() {
        SignatureVerifierDummy signatureVerifierDummy = new SignatureVerifierDummy();
        signatureVerifierDummy.verifySignature(null, null);
    }
}
