// SPDX-License-Identifier: Apache-2.0
package org.hiero.block.signing;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.cryptography.tss.TSS;
import com.hedera.cryptography.wraps.WRAPSLibraryBridge;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.node.transaction.SignedTransaction;
import com.hedera.hapi.node.transaction.TransactionBody;
import com.hedera.hapi.node.tss.LedgerIdPublicationTransactionBody;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import org.hiero.block.api.RosterEntry;
import org.hiero.block.api.TssData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/// Verifies that [TssBlockSigner] produces signatures the block-verification `TSSVerifier` accepts.
///
/// The tests drive the exact library call the verifier performs, `TSS.verifyTSS(ledgerId, signature,
/// rootHash)` with the ledger id taken from the signer's [TssData], so a passing test means the real
/// verifier accepts the proof. No other library state is needed: the signature carries the hinTS
/// verification key and the WRAPS proof, and the library embeds the parameters it checks the proof with.
class TssBlockSignerTest {

    /// Tests of the block signatures the signer produces.
    @Nested
    @DisplayName("Block signatures")
    class BlockSignatureTests {
        /// This test aims to assert that a signed block proof carries a complete TSS signature, that is
        /// the hinTS verification key, the hinTS aggregate signature and the compressed WRAPS proof
        /// concatenated, with the requested block number, and that `TSS.verifyTSS` accepts it for the
        /// signer's ledger id and the signed block root hash.
        @Test
        @DisplayName("signBlockProof() produces a signature accepted by TSS.verifyTSS()")
        void signatureIsAcceptedByVerifyTss() {
            final TssBlockSigner signer = TssBlockSigner.create();
            final Bytes blockRootHash = sha384("block-root-hash");

            final BlockProof proof = signer.signBlockProof(7L, blockRootHash);

            assertThat(proof.block()).isEqualTo(7L);
            assertThat(proof.hasSignedBlockProof()).isTrue();
            final Bytes signature = proof.signedBlockProof().blockSignature();
            assertThat(signature.length()).isEqualTo(TssBlockSigner.TSS_SIGNATURE_LENGTH);
            assertThat(verifies(signer, signature, blockRootHash)).isTrue();
        }

        /// This test aims to assert that a signature over one block root hash is rejected by
        /// `TSS.verifyTSS` when checked against a different hash, so the hinTS aggregate signature
        /// really binds the signed hash.
        @Test
        @DisplayName("a signature does not verify for a different block root hash")
        void signatureDoesNotVerifyForADifferentBlockRootHash() {
            final TssBlockSigner signer = TssBlockSigner.create();
            final BlockProof proof = signer.signBlockProof(1L, sha384("the-signed-root"));
            final Bytes signature = proof.signedBlockProof().blockSignature();

            assertThat(verifies(signer, signature, sha384("a-different-root"))).isFalse();
        }

        /// This test aims to assert that flipping one byte of the hinTS aggregate signature part of a
        /// signature makes `TSS.verifyTSS` reject it.
        @Test
        @DisplayName("a tampered hinTS signature does not verify")
        void tamperedHintsSignatureDoesNotVerify() {
            final TssBlockSigner signer = TssBlockSigner.create();
            final Bytes blockRootHash = sha384("block-root-hash");
            final byte[] signature = signer.signBlockProof(1L, blockRootHash)
                    .signedBlockProof()
                    .blockSignature()
                    .toByteArray();
            signature[TssBlockSigner.HINTS_VERIFICATION_KEY_LENGTH + 5] ^= 0x01;

            assertThat(verifies(signer, Bytes.wrap(signature), blockRootHash)).isFalse();
        }

        /// This test aims to assert that flipping one byte of the compressed WRAPS proof part of a
        /// signature makes `TSS.verifyTSS` reject it, so the proof is really checked on every verification.
        @Test
        @DisplayName("a tampered WRAPS proof does not verify")
        void tamperedWrapsProofDoesNotVerify() {
            final TssBlockSigner signer = TssBlockSigner.create();
            final Bytes blockRootHash = sha384("block-root-hash");
            final byte[] signature = signer.signBlockProof(1L, blockRootHash)
                    .signedBlockProof()
                    .blockSignature()
                    .toByteArray();
            signature[signature.length - 1] ^= 0x01;

            assertThat(verifies(signer, Bytes.wrap(signature), blockRootHash)).isFalse();
        }
    }

    /// Tests of the verification material paired with the signatures.
    @Nested
    @DisplayName("Verification material")
    class VerificationMaterialTests {
        /// This test aims to assert that the signer's [TssData] carries a 64 byte ledger id that is
        /// the network id of its roster and hinTS verification key, as computed by the WRAPS library,
        /// a single-entry roster, no WRAPS verification key, and is valid from block 0.
        @Test
        @DisplayName("TssData carries the network id of the roster and its hinTS key")
        void tssDataCarriesTheNetworkId() {
            final TssBlockSigner signer = TssBlockSigner.create();
            final TssData tssData = signer.verificationMaterial().tssData();
            final List<RosterEntry> entries = tssData.currentRoster().rosterEntries();
            final byte[] hintsVerificationKey = signer.signBlockProof(1L, sha384("root"))
                    .signedBlockProof()
                    .blockSignature()
                    .slice(0, TssBlockSigner.HINTS_VERIFICATION_KEY_LENGTH)
                    .toByteArray();

            assertThat(tssData.ledgerId().length()).isEqualTo(TssBlockSigner.LEDGER_ID_LENGTH);
            assertThat(entries).hasSize(1);
            assertThat(tssData.wrapsVerificationKey().length()).isZero();
            assertThat(tssData.validFromBlock()).isZero();
            final byte[] networkId = WRAPSLibraryBridge.getInstance()
                    .computeNetworkID(
                            new byte[][] {entries.getFirst().schnorrPublicKey().toByteArray()},
                            new long[] {entries.getFirst().weight()},
                            new long[] {entries.getFirst().nodeId()},
                            hintsVerificationKey);
            assertThat(tssData.ledgerId().toByteArray()).isEqualTo(networkId);
        }

        /// This test aims to assert that every signer created in a JVM derives the same roster and
        /// ledger id, so several publishers in one test share one chain of trust and a verifier
        /// provisioned from any of them accepts blocks from all of them.
        @Test
        @DisplayName("signers are deterministic")
        void signersAreDeterministic() {
            final TssBlockSigner first = TssBlockSigner.create();
            final TssBlockSigner second = TssBlockSigner.create();
            final Bytes blockRootHash = sha384("root");

            assertThat(second.verificationMaterial().tssData())
                    .isEqualTo(first.verificationMaterial().tssData());
            assertThat(verifies(
                            first,
                            second.signBlockProof(3L, blockRootHash)
                                    .signedBlockProof()
                                    .blockSignature(),
                            blockRootHash))
                    .isTrue();
        }

        /// This test aims to assert that the genesis ledger id publication the signer builds for
        /// block 0 parses as a signed transaction whose body carries the signer's ledger id and its
        /// roster entry as a node contribution, and no history proof verification key, matching what
        /// a consensus node publishes.
        @Test
        @DisplayName("the genesis publication carries the ledger id and roster, no verification key")
        void genesisPublicationCarriesLedgerIdAndRoster() throws ParseException {
            final TssBlockSigner signer = TssBlockSigner.create();
            final TssData tssData = signer.verificationMaterial().tssData();

            final SignedTransaction signedTransaction =
                    SignedTransaction.PROTOBUF.parse(signer.genesisLedgerIdSignedTransaction());
            final TransactionBody body = TransactionBody.PROTOBUF.parse(signedTransaction.bodyBytes());
            final LedgerIdPublicationTransactionBody publication = body.ledgerIdPublicationOrThrow();

            assertThat(publication.ledgerId()).isEqualTo(tssData.ledgerId());
            assertThat(publication.historyProofVerificationKey().length()).isZero();
            assertThat(publication.nodeContributions()).hasSize(1);
            assertThat(publication.nodeContributions().getFirst().historyProofKey())
                    .isEqualTo(
                            tssData.currentRoster().rosterEntries().getFirst().schnorrPublicKey());
        }
    }

    /// Tests of the committed genesis WRAPS proof resource.
    @Nested
    @DisplayName("Committed genesis WRAPS proof")
    class CommittedProofTests {
        /// This test aims to assert that the committed genesis WRAPS proof (the last part of every
        /// signature) verifies on its own with `WRAPSLibraryBridge.verifyCompressedProof` against the
        /// signer's ledger id and hinTS verification key, which is what the composite verification
        /// relies on.
        @Test
        @DisplayName("the committed proof verifies against the ledger id and hinTS key")
        void committedProofVerifies() {
            final TssBlockSigner signer = TssBlockSigner.create();
            final byte[] signature = signer.signBlockProof(1L, sha384("root"))
                    .signedBlockProof()
                    .blockSignature()
                    .toByteArray();
            final byte[] hintsVerificationKey =
                    Arrays.copyOfRange(signature, 0, TssBlockSigner.HINTS_VERIFICATION_KEY_LENGTH);
            final byte[] wrapsProof = Arrays.copyOfRange(
                    signature,
                    TssBlockSigner.TSS_SIGNATURE_LENGTH - TssBlockSigner.COMPRESSED_WRAPS_PROOF_LENGTH,
                    signature.length);
            final byte[] ledgerId =
                    signer.verificationMaterial().tssData().ledgerId().toByteArray();

            assertThat(wrapsProof).hasSize(TssBlockSigner.COMPRESSED_WRAPS_PROOF_LENGTH);
            assertThat(WRAPSLibraryBridge.getInstance()
                            .verifyCompressedProof(wrapsProof, ledgerId, hintsVerificationKey))
                    .isTrue();
        }

        /// This test aims to assert that the committed genesis WRAPS proof only establishes the hinTS
        /// verification key it was constructed for: verifying it against another key fails.
        @Test
        @DisplayName("the committed proof does not verify for another hinTS key")
        void committedProofDoesNotVerifyForAnotherHintsKey() {
            final TssBlockSigner signer = TssBlockSigner.create();
            final byte[] signature = signer.signBlockProof(1L, sha384("root"))
                    .signedBlockProof()
                    .blockSignature()
                    .toByteArray();
            final byte[] otherHintsVerificationKey = new byte[TssBlockSigner.HINTS_VERIFICATION_KEY_LENGTH];
            Arrays.fill(otherHintsVerificationKey, (byte) 0x5a);
            final byte[] wrapsProof = Arrays.copyOfRange(
                    signature,
                    TssBlockSigner.TSS_SIGNATURE_LENGTH - TssBlockSigner.COMPRESSED_WRAPS_PROOF_LENGTH,
                    signature.length);
            final byte[] ledgerId =
                    signer.verificationMaterial().tssData().ledgerId().toByteArray();

            assertThat(WRAPSLibraryBridge.getInstance()
                            .verifyCompressedProof(wrapsProof, ledgerId, otherHintsVerificationKey))
                    .isFalse();
        }
    }

    /// Runs the verifier's library call for a signer's ledger id.
    private static boolean verifies(final TssBlockSigner signer, final Bytes signature, final Bytes blockRootHash) {
        final byte[] ledgerId =
                signer.verificationMaterial().tssData().ledgerId().toByteArray();
        return TSS.verifyTSS(ledgerId, signature.toByteArray(), blockRootHash.toByteArray());
    }

    private static Bytes sha384(final String text) {
        try {
            return Bytes.wrap(MessageDigest.getInstance("SHA-384").digest(text.getBytes()));
        } catch (final NoSuchAlgorithmException fatal) {
            throw new IllegalStateException(fatal);
        }
    }
}
