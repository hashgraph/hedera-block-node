/*
 * Copyright (C) 2024-2025 Hedera Hashgraph, LLC
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

package com.hedera.block.server.verification.signature;

import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Inject;

/**
 * A dummy implementation of {@link SignatureVerifier} that always returns true.
 */
public class SignatureVerifierDummy implements SignatureVerifier {

    /**
     * Constructs the dummy verifier.
     */
    @Inject
    // on actual impl we would need to provide the public key (aka LedgerID)
    public SignatureVerifierDummy() {}

    /**
     * Verifies the signature of a hash, for the dummy implementation this always returns true.
     *
     * @param hash the hash to verify
     * @param signature the signature to verify
     * @return true if the signature is valid, false otherwise
     */
    @Override
    public Boolean verifySignature(@NonNull Bytes hash, @NonNull Bytes signature) {
        // Dummy implementation that always returns true
        return true;
    }
}
