// SPDX-License-Identifier: Apache-2.0
package org.hiero.block.node.block.verification.verifier;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hiero.block.api.TssData;
import org.hiero.block.api.TssRoster;
import org.hiero.block.node.app.fixtures.TestConfigurationBuilder;
import org.hiero.block.node.app.fixtures.TestUtils;
import org.hiero.block.node.app.fixtures.async.BlockingExecutor;
import org.hiero.block.node.app.fixtures.async.ScheduledBlockingExecutor;
import org.hiero.block.node.app.fixtures.async.TestThreadPoolManager;
import org.hiero.block.node.app.fixtures.blocks.TestBlock;
import org.hiero.block.node.app.fixtures.blocks.TestBlockBuilder;
import org.hiero.block.node.block.verification.NativeVerificationLibrary;
import org.hiero.block.node.block.verification.VerificationDataProvider;
import org.hiero.block.node.block.verification.hasher.BlockHasher;
import org.hiero.block.node.block.verification.hasher.HashingResult;
import org.hiero.block.node.block.verification.metrics.MetricsHolder;
import org.hiero.block.node.block.verification.session.SessionFailureType;
import org.hiero.block.node.spi.BlockNodeContext;
import org.hiero.block.node.spi.blockmessaging.BlockItems;
import org.hiero.block.node.spi.blockmessaging.BlockSource;
import org.hiero.block.signing.TssBlockSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// End-to-end test that a [TssBlockSigner] signature is accepted by the real [TSSVerifier].
///
/// Generates the roster and keys locally, provisions the real [VerificationDataProvider] with the
/// signer's [org.hiero.block.signing.VerificationMaterial], signs the root hash that [BlockHasher]
/// computes for a locally built block, and confirms the production verifier accepts it and rejects
/// tampering. It also covers which TSS data the provider accepts: only data whose ledger id has the
/// length the library verifies against.
@Timeout(unit = SECONDS, value = 30)
@DisplayName("Signer -> TSSVerifier end-to-end")
class SignerTssVerificationTest {

    private MetricsHolder metricsHolder;
    private VerificationDataProvider verificationDataProvider;

    @BeforeEach
    void setUp() {
        final BlockNodeContext context = TestUtils.testContext(
                new TestConfigurationBuilder().getOrCreateConfig(),
                new TestThreadPoolManager<>(
                        new BlockingExecutor(new LinkedBlockingQueue<>()),
                        new ScheduledBlockingExecutor(new LinkedBlockingQueue<>())));
        metricsHolder = MetricsHolder.create(context.metricRegistry());
        verificationDataProvider = new VerificationDataProvider(context);
    }

    /// Tests of signatures verified by the real [TSSVerifier].
    @Nested
    @DisplayName("TSSVerifier")
    class VerifierTests {
        /// This test aims to assert that a block signed by the locally generated signer, whose
        /// signature carries the hinTS verification key, the hinTS signature over the hashed root
        /// and the committed WRAPS proof, is accepted by the real verifier once the provider holds
        /// the signer's TSS data, and that the signature has the single supported layout length.
        @Test
        @DisplayName("verify() accepts a locally signed block")
        void verifyAcceptsLocallySignedBlock() {
            final TestBlock block = TestBlockBuilder.generateBlockWithNumber(3L);
            final Bytes rootHash = runHashing(block).rootHash();

            final TssBlockSigner signer = TssBlockSigner.create();
            verificationDataProvider.safeUpdateTssData(
                    signer.verificationMaterial().tssData(), false);
            final Bytes signature = signer.signBlockProof(block.number(), rootHash)
                    .signedBlockProof()
                    .blockSignature();
            assertThat(signature.length())
                    .withFailMessage("TSS signature must be hinTS key, hinTS signature and WRAPS proof")
                    .isEqualTo(TssBlockSigner.TSS_SIGNATURE_LENGTH);

            final TSSVerifier verifier = new TSSVerifier(
                    metricsHolder.proofVerificationMetrics(), rootHash, signature, verificationDataProvider);

            assertThat(verifier.verify())
                    .withFailMessage("Locally signed TSS block should verify successfully")
                    .isNull();
        }

        /// This test aims to assert that a valid signature over one root hash is reported as a bad
        /// block proof when verified against a different root hash.
        @Test
        @DisplayName("verify() rejects the signature against a different root hash")
        void verifyRejectsMismatchedRootHash() {
            final TestBlock block = TestBlockBuilder.generateBlockWithNumber(3L);
            final Bytes rootHash = runHashing(block).rootHash();

            final TssBlockSigner signer = TssBlockSigner.create();
            verificationDataProvider.safeUpdateTssData(
                    signer.verificationMaterial().tssData(), false);
            final Bytes signature = signer.signBlockProof(block.number(), rootHash)
                    .signedBlockProof()
                    .blockSignature();

            final byte[] tamperedHash = rootHash.toByteArray();
            tamperedHash[0] = (byte) ~tamperedHash[0];
            final TSSVerifier verifier = new TSSVerifier(
                    metricsHolder.proofVerificationMetrics(),
                    Bytes.wrap(tamperedHash),
                    signature,
                    verificationDataProvider);

            assertThat(verifier.verify()).isEqualTo(SessionFailureType.BAD_BLOCK_PROOF);
        }

        /// This test aims to assert that a signature whose hinTS verification key part was altered is
        /// reported as a bad block proof: the WRAPS proof no longer matches the key it should establish.
        @Test
        @DisplayName("verify() rejects a tampered hinTS verification key")
        void verifyRejectsTamperedVerificationKey() {
            final TestBlock block = TestBlockBuilder.generateBlockWithNumber(3L);
            final Bytes rootHash = runHashing(block).rootHash();

            final TssBlockSigner signer = TssBlockSigner.create();
            verificationDataProvider.safeUpdateTssData(
                    signer.verificationMaterial().tssData(), false);
            final byte[] signature = signer.signBlockProof(block.number(), rootHash)
                    .signedBlockProof()
                    .blockSignature()
                    .toByteArray();
            signature[0] = (byte) ~signature[0];

            final TSSVerifier verifier = new TSSVerifier(
                    metricsHolder.proofVerificationMetrics(),
                    rootHash,
                    Bytes.wrap(signature),
                    verificationDataProvider);

            assertThat(verifier.verify()).isEqualTo(SessionFailureType.BAD_BLOCK_PROOF);
        }

        /// This test aims to assert that a signature whose WRAPS proof part was altered is reported as
        /// a bad block proof, so the chain of trust proof is checked on every verification.
        @Test
        @DisplayName("verify() rejects a tampered WRAPS proof")
        void verifyRejectsTamperedWrapsProof() {
            final TestBlock block = TestBlockBuilder.generateBlockWithNumber(3L);
            final Bytes rootHash = runHashing(block).rootHash();

            final TssBlockSigner signer = TssBlockSigner.create();
            verificationDataProvider.safeUpdateTssData(
                    signer.verificationMaterial().tssData(), false);
            final byte[] signature = signer.signBlockProof(block.number(), rootHash)
                    .signedBlockProof()
                    .blockSignature()
                    .toByteArray();
            signature[signature.length - 1] = (byte) ~signature[signature.length - 1];

            final TSSVerifier verifier = new TSSVerifier(
                    metricsHolder.proofVerificationMetrics(),
                    rootHash,
                    Bytes.wrap(signature),
                    verificationDataProvider);

            assertThat(verifier.verify()).isEqualTo(SessionFailureType.BAD_BLOCK_PROOF);
        }

        /// This test aims to assert that a signature truncated to the key and hinTS signature alone,
        /// with no chain of trust proof, is reported as a bad block proof rather than failing the
        /// session: the library rejects the malformed layout with an exception that the verifier
        /// turns into the failure type.
        @Test
        @DisplayName("verify() reports a malformed signature as a bad block proof")
        void verifyReportsMalformedSignatureAsBadBlockProof() {
            final TestBlock block = TestBlockBuilder.generateBlockWithNumber(3L);
            final Bytes rootHash = runHashing(block).rootHash();

            final TssBlockSigner signer = TssBlockSigner.create();
            verificationDataProvider.safeUpdateTssData(
                    signer.verificationMaterial().tssData(), false);
            final Bytes signature = signer.signBlockProof(block.number(), rootHash)
                    .signedBlockProof()
                    .blockSignature()
                    .slice(0, TssBlockSigner.HINTS_VERIFICATION_KEY_LENGTH + TssBlockSigner.HINTS_SIGNATURE_LENGTH);

            final TSSVerifier verifier = new TSSVerifier(
                    metricsHolder.proofVerificationMetrics(), rootHash, signature, verificationDataProvider);

            assertThat(verifier.verify()).isEqualTo(SessionFailureType.BAD_BLOCK_PROOF);
        }
    }

    /// Tests of the TSS data the provider accepts for verification.
    @Nested
    @DisplayName("VerificationDataProvider TSS data")
    class ProviderTests {
        /// This test aims to assert that the signer's TSS data, whose ledger id has the length the
        /// library verifies against, is installed by the provider and reported as the current data.
        @Test
        @DisplayName("TSS data with a 64 byte ledger id is installed")
        void installsTssDataWithCurrentLedgerIdLength() {
            final TssData tssData =
                    TssBlockSigner.create().verificationMaterial().tssData();

            verificationDataProvider.safeUpdateTssData(tssData, false);

            assertThat(tssData.ledgerId().length()).isEqualTo(NativeVerificationLibrary.LEDGER_ID_LENGTH);
            assertThat(verificationDataProvider.hasTssData()).isTrue();
            assertThat(verificationDataProvider.currentTssData()).isEqualTo(tssData);
        }

        /// This test aims to assert that TSS data with a 32 byte ledger id, as persisted by a Block
        /// Node or served by a peer that predates the current TSS library, is ignored: the provider
        /// keeps no data, so block 0 of the stream can still self-provision the current ledger id.
        @Test
        @DisplayName("TSS data with a 32 byte ledger id is ignored")
        void ignoresTssDataWithStaleLedgerIdLength() {
            final TssData current =
                    TssBlockSigner.create().verificationMaterial().tssData();
            final TssData stale = current.copyBuilder()
                    .ledgerId(current.ledgerId().slice(0, 32))
                    .build();

            verificationDataProvider.safeUpdateTssData(stale, false);

            assertThat(verificationDataProvider.hasTssData()).isFalse();
            assertThat(verificationDataProvider.currentTssData()).isNull();
        }

        /// This test aims to assert that TSS data without a roster is still installed, since the
        /// verifier needs only the ledger id and the roster is informational.
        @Test
        @DisplayName("TSS data without a roster is installed")
        void installsTssDataWithoutRoster() {
            final TssData current =
                    TssBlockSigner.create().verificationMaterial().tssData();
            final TssData withoutRoster =
                    current.copyBuilder().currentRoster((TssRoster) null).build();

            verificationDataProvider.safeUpdateTssData(withoutRoster, false);

            assertThat(verificationDataProvider.currentTssData()).isEqualTo(withoutRoster);
        }

        /// This test aims to assert that TSS data valid from an earlier block than the current data
        /// is ignored, while different data valid from the same block replaces the current data,
        /// matching the rule the application state applies when it installs TSS data.
        @Test
        @DisplayName("older TSS data is ignored, same block data replaces the current data")
        void appliesValidFromBlockRule() {
            final TssData current =
                    TssBlockSigner.create().verificationMaterial().tssData();
            final TssData later = current.copyBuilder().validFromBlock(10L).build();
            final TssData sameBlockWithoutRoster =
                    later.copyBuilder().currentRoster((TssRoster) null).build();

            verificationDataProvider.safeUpdateTssData(later, false);
            verificationDataProvider.safeUpdateTssData(current, false);
            assertThat(verificationDataProvider.currentTssData()).isEqualTo(later);

            verificationDataProvider.safeUpdateTssData(sameBlockWithoutRoster, false);
            assertThat(verificationDataProvider.currentTssData()).isEqualTo(sameBlockWithoutRoster);
        }
    }

    private HashingResult runHashing(final TestBlock block) {
        final ConcurrentLinkedDeque<BlockItems> blockItemsDeque = new ConcurrentLinkedDeque<>();
        final BlockHasher hasher = new BlockHasher(
                TestBlockBuilder.BLOCK_HASH_ALGORITHM,
                new AtomicBoolean(false),
                blockItemsDeque,
                metricsHolder.hashingMetrics(),
                block.number(),
                BlockSource.PUBLISHER,
                verificationDataProvider);
        blockItemsDeque.add(block.asBlockItems());
        return hasher.get();
    }
}
