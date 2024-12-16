package com.hedera.block.server.verification.signature;

import com.hedera.pbj.runtime.io.buffer.Bytes;

import javax.inject.Inject;

public class SignatureVerifierDummy implements SignatureVerifier {

    @Inject
    // TODO we need to provide the public key (aka LedgerID)
    public SignatureVerifierDummy() {
    }

    @Override
    public Boolean verifySignature(Bytes hash, Bytes signature) {
        // Dummy implementation that always returns true
        return true;
    }
}
