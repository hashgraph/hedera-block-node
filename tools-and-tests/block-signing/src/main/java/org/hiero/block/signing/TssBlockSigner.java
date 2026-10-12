// SPDX-License-Identifier: Apache-2.0
package org.hiero.block.signing;

import com.hedera.cryptography.hints.AggregationAndVerificationKeys;
import com.hedera.cryptography.hints.HintsLibraryBridge;
import com.hedera.cryptography.tss.TSS;
import com.hedera.cryptography.wraps.Proof;
import com.hedera.cryptography.wraps.SchnorrKeys;
import com.hedera.cryptography.wraps.WRAPSLibraryBridge;
import com.hedera.cryptography.wraps.WRAPSLibraryBridge.SigningProtocolPhase;
import com.hedera.hapi.block.stream.BlockProof;
import com.hedera.hapi.block.stream.TssSignedBlockProof;
import com.hedera.hapi.node.transaction.SignedTransaction;
import com.hedera.hapi.node.transaction.TransactionBody;
import com.hedera.hapi.node.tss.LedgerIdNodeContribution;
import com.hedera.hapi.node.tss.LedgerIdPublicationTransactionBody;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.hiero.block.api.RosterEntry;
import org.hiero.block.api.TssData;
import org.hiero.block.api.TssRoster;

/// Produces TSS/hinTS threshold [BlockProof]s that the block-verification `TSSVerifier` accepts.
///
/// A block signature is `hintsVerificationKey || hintsSignature || compressedWrapsProof`
/// ([#TSS_SIGNATURE_LENGTH] bytes): the hinTS aggregate signature attests the block root hash, and
/// the WRAPS proof places the hinTS verification key in the chain of trust of the ledger id. The
/// proof is the same for every block the signer produces. Constructing it runs a SNARK prover that
/// takes tens of seconds, so it is computed once, offline, for a fixed roster by
/// [#generateGenesisWrapsProof] and committed as a resource that [#create] loads; verifying it only
/// needs the public parameters the TSS library embeds.
///
/// The signer uses a single-node roster (hinTS universe `n = 2`, the smallest valid power of two,
/// since the maximum number of parties is `n - 1`). All per-era key material, the hinTS CRS, BLS
/// secret, aggregation and verification keys, Schnorr keys and the ledger id, derives from a fixed
/// seed, so every instance in a JVM shares one roster: several simulator publishers sign
/// identically, a node can self-provision from any of them, and duplicate blocks are byte-identical
/// (and so detectable).
///
/// The ledger id is the 64 byte network id `WRAPSLibraryBridge.computeNetworkID(roster, hintsVerificationKey)`,
/// the hash of the address book followed by the hash of the hinTS verification key, which is what a
/// consensus node publishes in `LedgerIdPublicationTransactionBody.ledger_id`. The paired
/// [#verificationMaterial] is a [TssData] carrying that ledger id and the roster.
public final class TssBlockSigner implements BlockSigner {

    /// Length of a hinTS verification key.
    public static final int HINTS_VERIFICATION_KEY_LENGTH = 1096;

    /// Length of a hinTS aggregate signature.
    public static final int HINTS_SIGNATURE_LENGTH = HintsLibraryBridge.AGGREGATE_SIGNATURE_LENGTH_BYTES;

    /// Length of a compressed WRAPS proof (the library keeps its own constant private).
    public static final int COMPRESSED_WRAPS_PROOF_LENGTH = 11368;

    /// Length of a complete TSS block signature.
    public static final int TSS_SIGNATURE_LENGTH =
            HINTS_VERIFICATION_KEY_LENGTH + HINTS_SIGNATURE_LENGTH + COMPRESSED_WRAPS_PROOF_LENGTH;

    /// Length of a ledger id (the network id).
    public static final int LEDGER_ID_LENGTH = 64;

    /// hinTS universe size (power of two). Max parties = N - 1, so N = 2 supports the single node.
    private static final int N = 2;

    private static final long NODE_ID = 0L;
    private static final long WEIGHT = 1L;

    /// Fixed seed for the deterministic key ceremony; any constant works since these are test-only keys.
    private static final byte[] DETERMINISTIC_SEED = "hiero-block-signing-test-seed".getBytes();
    private static final int[] PARTIES = {0};
    private static final long[] WEIGHTS = {WEIGHT};
    private static final long[] NODE_IDS = {NODE_ID};

    /// Classpath resource holding the pre-computed genesis WRAPS proof for the deterministic roster.
    private static final String WRAPS_PROOF_RESOURCE = "/org/hiero/block/signing/genesis-wraps-proof.bin";

    private static final HintsLibraryBridge HINTS = HintsLibraryBridge.getInstance();
    private static final WRAPSLibraryBridge WRAPS = WRAPSLibraryBridge.getInstance();

    /// Serializes access to the JVM-wide hinTS/WRAPS native singletons above. They are shared by every
    /// signer instance and are not thread-safe, so the key ceremony (which resets the native cache) and
    /// signing must never overlap across instances in one JVM; the multi-publisher suites run several
    /// signers on separate threads in a single JVM.
    private static final ReentrantLock NATIVE_LOCK = new ReentrantLock();

    private final byte[] crs;
    private final byte[] hintsVerificationKey;
    private final byte[] aggregationKey;
    private final byte[] blsSecretKey;
    private final byte[] wrapsProof;
    private final byte[] ledgerId;
    private final byte[] schnorrPublicKey;
    private final VerificationMaterial verificationMaterial;

    private TssBlockSigner(final Era era, final byte[] wrapsProof) {
        this.crs = era.crs();
        this.hintsVerificationKey = era.hintsVerificationKey();
        this.aggregationKey = era.aggregationKey();
        this.blsSecretKey = era.blsSecretKey();
        this.wrapsProof = wrapsProof;
        this.ledgerId = era.ledgerId();
        this.schnorrPublicKey = era.schnorr().publicKey();
        this.verificationMaterial = VerificationMaterial.ofTss(TssData.newBuilder()
                .ledgerId(Bytes.wrap(ledgerId))
                .currentRoster(TssRoster.newBuilder()
                        .rosterEntries(List.of(RosterEntry.newBuilder()
                                .nodeId(NODE_ID)
                                .weight(WEIGHT)
                                .schnorrPublicKey(Bytes.wrap(schnorrPublicKey))
                                .build()))
                        .build())
                .validFromBlock(0L)
                .build());
    }

    /// Runs the deterministic key ceremony and returns a signer whose WRAPS proof is the committed
    /// resource generated by [#generateGenesisWrapsProof] over the same roster.
    ///
    /// The hinTS native library keeps singleton state cached from the most recent ceremony, so the
    /// cache is reset first. Every instance derives the same key material, so creating another
    /// instance never invalidates an existing one.
    public static TssBlockSigner create() {
        NATIVE_LOCK.lock();
        try {
            final Era era = buildEra(deterministicRandom());
            return new TssBlockSigner(era, loadCommittedWrapsProof());
        } finally {
            NATIVE_LOCK.unlock();
        }
    }

    /// Constructs the compressed genesis WRAPS proof for the deterministic roster.
    ///
    /// This is the slow step: it runs the SNARK prover over the public parameters the library embeds,
    /// which takes tens of seconds and a few gigabytes of native memory. It is meant to be run offline,
    /// once, to (re)generate the committed resource, not at test run time.
    ///
    /// The proof is a genesis proof: previous and next address book are both this roster, there is no
    /// previous proof, and the aggregate Schnorr signature is over the ledger id itself, the network id
    /// of this roster and its hinTS verification key. The returned proof is verified against that ledger
    /// id and key before it is returned.
    public static byte[] generateGenesisWrapsProof() {
        NATIVE_LOCK.lock();
        try {
            final SecureRandom random = deterministicRandom();
            final Era era = buildEra(random);
            final byte[][] schnorrPublicKeys = {era.schnorr().publicKey()};
            final byte[] aggregateSignature =
                    aggregateSchnorr(era.schnorr(), schnorrPublicKeys, era.ledgerId(), random32(random));
            final Proof proof = require(
                    "constructWrapsProof",
                    WRAPS.constructWrapsProof(
                            era.addressBookHash(),
                            schnorrPublicKeys,
                            WEIGHTS,
                            NODE_IDS,
                            schnorrPublicKeys,
                            WEIGHTS,
                            NODE_IDS,
                            null,
                            era.hintsVerificationKey(),
                            aggregateSignature));
            final byte[] compressed = proof.compressed();
            final byte[] result;
            if (compressed.length != COMPRESSED_WRAPS_PROOF_LENGTH) {
                throw new IllegalStateException("unexpected compressed WRAPS proof length " + compressed.length);
            } else if (!WRAPS.verifyCompressedProof(compressed, era.ledgerId(), era.hintsVerificationKey())) {
                throw new IllegalStateException("freshly constructed genesis WRAPS proof does not verify");
            } else {
                result = compressed;
            }
            return result;
        } finally {
            NATIVE_LOCK.unlock();
        }
    }

    private static SecureRandom deterministicRandom() {
        try {
            final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(DETERMINISTIC_SEED);
            return random;
        } catch (final NoSuchAlgorithmException fatal) {
            throw new IllegalStateException("SHA1PRNG unavailable", fatal);
        }
    }

    /// Runs the one-time key ceremony (roster, hinTS CRS, keys, ledger id). Callers hold [#NATIVE_LOCK];
    /// the `HINTS.resetCache()` here would corrupt an in-flight sign on the shared native singleton
    /// otherwise. The order of the random draws is part of the determinism: [#create] and
    /// [#generateGenesisWrapsProof] must derive the same era.
    private static Era buildEra(final SecureRandom random) {
        HINTS.resetCache();

        // Schnorr roster and its address book hash (the first half of the ledger id).
        final SchnorrKeys schnorr = require("generateSchnorrKeys", WRAPS.generateSchnorrKeys(random32(random)));
        final byte[][] schnorrPublicKeys = {schnorr.publicKey()};
        final byte[] addressBookHash =
                require("hashAddressBook", WRAPS.hashAddressBook(schnorrPublicKeys, WEIGHTS, NODE_IDS));

        // hinTS CRS ceremony: init -> at least one contribution -> prune to the universe size.
        byte[] crs = require("initCRS", HINTS.initCRS((short) N));
        crs = require("updateCRS", HINTS.updateCRS(crs, random32(random)));
        crs = require("pruneCRS", HINTS.pruneCRS(crs, (short) N));

        // hinTS per-party keys + preprocessing.
        final byte[] blsSecretKey = require("generateSecretKey", HINTS.generateSecretKey(random32(random)));
        final byte[] hintsPublicKey = require("computeHints", HINTS.computeHints(crs, blsSecretKey, 0, N));
        final byte[][] hintsPublicKeys = {hintsPublicKey};
        final AggregationAndVerificationKeys keys =
                require("preprocess", HINTS.preprocess(crs, PARTIES, hintsPublicKeys, WEIGHTS, N));

        // The ledger id is the network id of this roster and its hinTS verification key.
        final byte[] ledgerId = require(
                "computeNetworkID",
                WRAPS.computeNetworkID(schnorrPublicKeys, WEIGHTS, NODE_IDS, keys.verificationKey()));
        return new Era(
                crs, keys.verificationKey(), keys.aggregationKey(), blsSecretKey, addressBookHash, ledgerId, schnorr);
    }

    private static byte[] loadCommittedWrapsProof() {
        try (final InputStream in = TssBlockSigner.class.getResourceAsStream(WRAPS_PROOF_RESOURCE)) {
            final byte[] result;
            if (in == null) {
                throw new IllegalStateException("missing committed WRAPS proof resource " + WRAPS_PROOF_RESOURCE
                        + "; regenerate it with generateGenesisWrapsProof()");
            } else {
                final byte[] proof = in.readAllBytes();
                if (proof.length != COMPRESSED_WRAPS_PROOF_LENGTH) {
                    throw new IllegalStateException("committed WRAPS proof has length " + proof.length + ", expected "
                            + COMPRESSED_WRAPS_PROOF_LENGTH + "; regenerate it with generateGenesisWrapsProof()");
                } else {
                    result = proof;
                }
            }
            return result;
        } catch (final IOException fatal) {
            throw new UncheckedIOException("failed to read committed WRAPS proof", fatal);
        }
    }

    /// One-time deterministic key material of the roster.
    private record Era(
            byte[] crs,
            byte[] hintsVerificationKey,
            byte[] aggregationKey,
            byte[] blsSecretKey,
            byte[] addressBookHash,
            byte[] ledgerId,
            SchnorrKeys schnorr) {}

    @Override
    public BlockProof signBlockProof(final long blockNumber, final Bytes blockRootHash) {
        final byte[] message = blockRootHash.toByteArray();
        final byte[] tssSignature;
        NATIVE_LOCK.lock();
        try {
            final byte[] partialSignature = require("signBls", HINTS.signBls(message, blsSecretKey));
            final byte[] hintsSignature = require(
                    "aggregateSignatures",
                    HINTS.aggregateSignatures(
                            crs, aggregationKey, hintsVerificationKey, PARTIES, new byte[][] {partialSignature}));
            tssSignature = TSS.composeSignature(hintsVerificationKey, hintsSignature, wrapsProof);
        } finally {
            NATIVE_LOCK.unlock();
        }
        return BlockProof.newBuilder()
                .block(blockNumber)
                .signedBlockProof(TssSignedBlockProof.newBuilder()
                        .blockSignature(Bytes.wrap(tssSignature))
                        .build())
                .build();
    }

    @Override
    public VerificationMaterial verificationMaterial() {
        return verificationMaterial;
    }

    /// Builds the serialized `SignedTransaction` bytes carrying this signer's ledger id and roster as a
    /// `LedgerIdPublicationTransactionBody`.
    ///
    /// Placing this as a signed-transaction item inside block 0 lets a verifier self-provision its
    /// `TssData` from the block stream itself (see `BlockHasher.findLedgerIdPublication`), so no
    /// out-of-band bootstrap file is needed for a producer that streams from genesis. Like a consensus
    /// node, the publication carries no history proof verification key: the TSS library embeds the
    /// parameters it verifies WRAPS proofs with. The returned bytes are the value of the block item's
    /// signed-transaction field, portable across PBJ and protoc consumers.
    public Bytes genesisLedgerIdSignedTransaction() {
        final LedgerIdPublicationTransactionBody publication = LedgerIdPublicationTransactionBody.newBuilder()
                .ledgerId(Bytes.wrap(ledgerId))
                .nodeContributions(List.of(LedgerIdNodeContribution.newBuilder()
                        .nodeId(NODE_ID)
                        .weight(WEIGHT)
                        .historyProofKey(Bytes.wrap(schnorrPublicKey))
                        .build()))
                .build();
        final TransactionBody body =
                TransactionBody.newBuilder().ledgerIdPublication(publication).build();
        final SignedTransaction signedTransaction = SignedTransaction.newBuilder()
                .bodyBytes(TransactionBody.PROTOBUF.toBytes(body))
                .build();
        return SignedTransaction.PROTOBUF.toBytes(signedTransaction);
    }

    /// Runs the 4-phase threshold-Schnorr protocol for the single signer and returns the aggregate
    /// signature over `message`. R1 takes an empty roster; R2/R3/Aggregate take the full roster plus
    /// the prior rounds' messages. Only the genesis proof construction needs it.
    private static byte[] aggregateSchnorr(
            final SchnorrKeys schnorr, final byte[][] schnorrPublicKeys, final byte[] message, final byte[] entropy) {
        final boolean[] signers = {true};
        final byte[] privateKey = schnorr.privateKey();
        final byte[][] round1 = {
            require(
                    "R1",
                    WRAPS.runSigningProtocolPhase(
                            SigningProtocolPhase.R1,
                            entropy,
                            message,
                            privateKey,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null))
        };
        final byte[][] round2 = {
            require(
                    "R2",
                    WRAPS.runSigningProtocolPhase(
                            SigningProtocolPhase.R2,
                            entropy,
                            message,
                            privateKey,
                            schnorrPublicKeys,
                            WEIGHTS,
                            NODE_IDS,
                            signers,
                            round1,
                            null,
                            null))
        };
        final byte[][] round3 = {
            require(
                    "R3",
                    WRAPS.runSigningProtocolPhase(
                            SigningProtocolPhase.R3,
                            entropy,
                            message,
                            privateKey,
                            schnorrPublicKeys,
                            WEIGHTS,
                            NODE_IDS,
                            signers,
                            round1,
                            round2,
                            null))
        };
        return require(
                "Aggregate",
                WRAPS.runSigningProtocolPhase(
                        SigningProtocolPhase.Aggregate,
                        null,
                        message,
                        null,
                        schnorrPublicKeys,
                        WEIGHTS,
                        NODE_IDS,
                        signers,
                        round1,
                        round2,
                        round3));
    }

    private static byte[] random32(final SecureRandom random) {
        final byte[] bytes = new byte[WRAPSLibraryBridge.ENTROPY_SIZE];
        random.nextBytes(bytes);
        return bytes;
    }

    private static <T> T require(final String operation, final T result) {
        if (result == null) {
            throw new IllegalStateException("TSS native call '" + operation + "' returned null (invalid input)");
        } else {
            return result;
        }
    }
}
