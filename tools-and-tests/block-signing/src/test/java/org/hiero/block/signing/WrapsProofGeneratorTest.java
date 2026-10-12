// SPDX-License-Identifier: Apache-2.0
package org.hiero.block.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Offline (re)generator for the committed genesis WRAPS proof resource.
///
/// This is NOT a normal unit test: it is skipped unless the `GENERATE_WRAPS_PROOF=true` environment
/// variable is set, because it runs the WRAPS SNARK prover (tens of seconds and a few gigabytes of
/// native memory) and overwrites the committed resource. The prover needs nothing beyond the TSS
/// library jar: the public parameters are embedded in it.
///
/// Run it to (re)generate the resource, for example after a `hedera-cryptography` upgrade that changes
/// the proof format or the ledger id derivation, with:
/// ```
/// GENERATE_WRAPS_PROOF=true ./gradlew :block-signing:test --tests '*WrapsProofGeneratorTest*'
/// ```
/// then commit the updated `src/main/resources/org/hiero/block/signing/genesis-wraps-proof.bin`.
class WrapsProofGeneratorTest {

    private static final Path RESOURCE_PATH =
            Path.of("src", "main", "resources", "org", "hiero", "block", "signing", "genesis-wraps-proof.bin");

    /// This test aims to assert that, when regeneration is requested, the genesis WRAPS proof
    /// constructed for the deterministic roster has the compressed proof length and is written to
    /// the committed resource path; the generator verifies the proof against the roster's ledger id
    /// and hinTS verification key before returning it.
    @Test
    @DisplayName("regenerates the committed genesis WRAPS proof when GENERATE_WRAPS_PROOF=true")
    void regeneratesCommittedGenesisWrapsProof() throws IOException {
        assumeTrue(
                "true".equals(System.getenv("GENERATE_WRAPS_PROOF")),
                "set GENERATE_WRAPS_PROOF=true to regenerate the committed WRAPS proof");

        final byte[] compressedProof = TssBlockSigner.generateGenesisWrapsProof();
        assertThat(compressedProof).hasSize(TssBlockSigner.COMPRESSED_WRAPS_PROOF_LENGTH);

        Files.createDirectories(RESOURCE_PATH.getParent());
        Files.write(RESOURCE_PATH, compressedProof);
    }
}
