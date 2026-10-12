// SPDX-License-Identifier: Apache-2.0
package org.hiero.block.node.block.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hedera.cryptography.wraps.WRAPSLibraryBridge;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import org.hiero.block.signing.TssBlockSigner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/// Tests of the [NativeVerificationLibrary] gateway and of the TSS library it fronts, using a real
/// WRAPS proof produced by a consensus node running the same library version.
///
/// The vector under `src/test/resources/tss/cn-3.18.0-ledgerA/` is the genesis chain of trust proof
/// of a single node consensus network captured by the consensus node project (see the README next to
/// it): the 64 byte ledger id, the 1096 byte hinTS verification key the proof establishes, and the
/// 11368 byte compressed WRAPS proof. Verifying it here proves the Block Node's library build accepts
/// proofs made by a consensus node, independent of the locally generated test signer.
@DisplayName("NativeVerificationLibrary")
class NativeVerificationLibraryTest {

    private static final String VECTOR_DIRECTORY = "/tss/cn-3.18.0-ledgerA/";

    /// Tests of loading the embedded WRAPS parameters.
    @Nested
    @DisplayName("warmUp()")
    class WarmUpTests {
        /// This test aims to assert that the embedded WRAPS public parameters can be extracted and
        /// loaded in this process, so WRAPS proof verification is available, and that asking again
        /// gives the same answer.
        @Test
        @DisplayName("loads the embedded WRAPS parameters")
        void loadsEmbeddedParameters() {
            assertThat(NativeVerificationLibrary.warmUp()).isTrue();
            assertThat(NativeVerificationLibrary.warmUp()).isTrue();
        }
    }

    /// Tests of a chain of trust proof produced by a consensus node.
    @Nested
    @DisplayName("Consensus node WRAPS proof")
    class ConsensusNodeProofTests {
        /// This test aims to assert that the compressed WRAPS proof a consensus node built for its
        /// genesis roster verifies, with the library the Block Node ships, against the ledger id and
        /// hinTS verification key that node published.
        @Test
        @DisplayName("the captured proof verifies against its ledger id and hinTS key")
        void capturedProofVerifies() {
            final byte[] ledgerId = vector("ledger-id.bin");
            final byte[] hintsVerificationKey = vector("hints-verification-key.bin");
            final byte[] wrapsProof = vector("wraps-proof.bin");

            assertThat(ledgerId).hasSize(NativeVerificationLibrary.LEDGER_ID_LENGTH);
            assertThat(hintsVerificationKey).hasSize(TssBlockSigner.HINTS_VERIFICATION_KEY_LENGTH);
            assertThat(wrapsProof).hasSize(TssBlockSigner.COMPRESSED_WRAPS_PROOF_LENGTH);
            assertThat(WRAPSLibraryBridge.getInstance()
                            .verifyCompressedProof(wrapsProof, ledgerId, hintsVerificationKey))
                    .isTrue();
        }

        /// This test aims to assert that the captured proof is rejected when a single byte of it is
        /// altered, so a passing verification is not an artifact of lenient parsing.
        @Test
        @DisplayName("a tampered captured proof does not verify")
        void tamperedCapturedProofDoesNotVerify() {
            final byte[] ledgerId = vector("ledger-id.bin");
            final byte[] hintsVerificationKey = vector("hints-verification-key.bin");
            final byte[] wrapsProof = vector("wraps-proof.bin");
            wrapsProof[wrapsProof.length / 2] ^= 0x01;

            assertThat(WRAPSLibraryBridge.getInstance()
                            .verifyCompressedProof(wrapsProof, ledgerId, hintsVerificationKey))
                    .isFalse();
        }

        /// This test aims to assert that the captured proof only establishes the hinTS verification
        /// key it was built for and only within the chain of trust of its own ledger id: verifying it
        /// against another key or another ledger id fails.
        @Test
        @DisplayName("the captured proof does not verify for another key or ledger id")
        void capturedProofIsBoundToItsKeyAndLedgerId() {
            final byte[] ledgerId = vector("ledger-id.bin");
            final byte[] hintsVerificationKey = vector("hints-verification-key.bin");
            final byte[] wrapsProof = vector("wraps-proof.bin");
            final byte[] otherKey = hintsVerificationKey.clone();
            otherKey[10] ^= 0x01;

            assertThat(WRAPSLibraryBridge.getInstance().verifyCompressedProof(wrapsProof, ledgerId, otherKey))
                    .isFalse();
        }

        /// This test aims to assert that the captured proof only verifies within the chain of trust
        /// of its own ledger id: a ledger id whose genesis address book hash (the first 32 bytes)
        /// differs is rejected.
        @Test
        @DisplayName("the captured proof does not verify for another genesis address book hash")
        void capturedProofIsBoundToItsAddressBookHash() {
            final byte[] ledgerId = vector("ledger-id.bin");
            final byte[] hintsVerificationKey = vector("hints-verification-key.bin");
            final byte[] wrapsProof = vector("wraps-proof.bin");
            final byte[] otherLedgerId = ledgerId.clone();
            otherLedgerId[8] ^= 0x01;

            assertThat(WRAPSLibraryBridge.getInstance()
                            .verifyCompressedProof(wrapsProof, otherLedgerId, hintsVerificationKey))
                    .isFalse();
        }

        /// This test aims to pin a property of the library the Block Node relies on when it treats
        /// the ledger id as opaque: compressed WRAPS verification anchors the chain of trust in the
        /// genesis address book hash half of the ledger id and checks the hinTS verification key it
        /// is given, while the genesis hinTS key hash half of the ledger id is not an input of the
        /// verification, so altering it does not change the outcome. Should a later library version
        /// start checking that half, this test flags the behaviour change.
        @Test
        @DisplayName("the captured proof verification does not read the genesis hinTS key hash half")
        void capturedProofVerificationIgnoresGenesisKeyHashHalf() {
            final byte[] ledgerId = vector("ledger-id.bin");
            final byte[] hintsVerificationKey = vector("hints-verification-key.bin");
            final byte[] wrapsProof = vector("wraps-proof.bin");
            final byte[] otherLedgerId = ledgerId.clone();
            otherLedgerId[40] ^= 0x01;

            assertThat(WRAPSLibraryBridge.getInstance()
                            .verifyCompressedProof(wrapsProof, otherLedgerId, hintsVerificationKey))
                    .isTrue();
        }
    }

    /// Tests of the composite signature verification entry point.
    @Nested
    @DisplayName("verifyTss()")
    class VerifyTssTests {
        /// This test aims to assert that a ledger id of the length used before the current library
        /// (32 bytes) is rejected by the library with an `IllegalArgumentException`, which is the
        /// reason the data provider never installs such data.
        @Test
        @DisplayName("rejects a 32 byte ledger id")
        void rejectsStaleLedgerIdLength() {
            final byte[] staleLedgerId = Arrays.copyOf(vector("ledger-id.bin"), 32);
            final byte[] signature = new byte[TssBlockSigner.TSS_SIGNATURE_LENGTH];
            final byte[] hash = new byte[32];

            assertThatThrownBy(() -> NativeVerificationLibrary.verifyTss(staleLedgerId, signature, hash))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        /// This test aims to assert that a signature which is not the exact concatenation of a hinTS
        /// verification key, a hinTS signature and a compressed WRAPS proof is rejected by the library
        /// with an `IllegalArgumentException` rather than silently verified.
        @Test
        @DisplayName("rejects a signature with an unsupported layout")
        void rejectsUnsupportedSignatureLayout() {
            final byte[] ledgerId = vector("ledger-id.bin");
            final byte[] tooShort = new byte[TssBlockSigner.TSS_SIGNATURE_LENGTH - 1];
            final byte[] hash = new byte[32];

            assertThatThrownBy(() -> NativeVerificationLibrary.verifyTss(ledgerId, tooShort, hash))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static byte[] vector(final String name) {
        try (final InputStream in = NativeVerificationLibraryTest.class.getResourceAsStream(VECTOR_DIRECTORY + name)) {
            final byte[] result;
            if (in == null) {
                throw new IllegalStateException("missing test vector " + VECTOR_DIRECTORY + name);
            } else {
                result = in.readAllBytes();
            }
            return result;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
