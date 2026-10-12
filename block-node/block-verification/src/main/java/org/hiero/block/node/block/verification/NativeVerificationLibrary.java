// SPDX-License-Identifier: Apache-2.0
package org.hiero.block.node.block.verification;

import static java.lang.System.Logger.Level.WARNING;

import com.hedera.cryptography.hints.HintsLibraryBridge;
import com.hedera.cryptography.tss.TSS;
import com.hedera.cryptography.wraps.WRAPSLibraryBridge;
import java.util.concurrent.locks.ReentrantLock;

/// Serializing gateway to the hinTS/WRAPS native cryptography library (`com.hedera.cryptography`).
///
/// A TSS block signature is the concatenation of the hinTS verification key, the hinTS aggregate
/// signature over the block root hash, and the compressed WRAPS proof that places that verification
/// key in the chain of trust of the network's ledger id. `TSS.verifyTSS` checks all three parts; the
/// only input it needs besides the signature and the signed hash is the ledger id, the 64 byte network
/// id a consensus network computes over its genesis address book and genesis hinTS verification key
/// (`WRAPSLibraryBridge.computeNetworkID`).
///
/// The library embeds the WRAPS public parameters it verifies proofs with. On first use it extracts them
/// (about 64 MB) into the directory named by the `java.io.tmpdir` system property and loads them natively.
/// [#warmUp] triggers that once at plugin start, so the first block does not pay for it and a temp
/// directory that cannot be written to is noticed immediately instead of failing every block proof.
///
/// The native library is a JVM-wide singleton and is **not** thread-safe. Verification sessions run
/// concurrently, one per block on a virtual-thread executor, and empirically even two concurrent
/// `verifyTSS` calls corrupt the library's shared native state, producing sporadic, load-dependent
/// `BAD_BLOCK_PROOF` failures for signatures that are individually valid.
///
/// Every native call in this module therefore runs under a single process-wide [ReentrantLock] owned
/// here, so callers never manage locking themselves. This serializes verification throughput, an
/// acceptable trade-off given the library constraint: correctness takes precedence over throughput.
/// The lock cost is negligible next to the native verify it guards.
public final class NativeVerificationLibrary {
    /// Logger for the gateway.
    private static final System.Logger LOGGER = System.getLogger(NativeVerificationLibrary.class.getName());

    /// Length in bytes of a ledger id: the hash of the genesis address book followed by the hash of
    /// the genesis hinTS verification key, as computed by `WRAPSLibraryBridge.computeNetworkID`. The
    /// library exposes no public constant for it, but `TSS.verifyTSS` rejects every other length.
    public static final int LEDGER_ID_LENGTH = 64;

    /// The single lock guarding every hinTS/WRAPS native call in this module.
    private static final ReentrantLock LOCK = new ReentrantLock();

    private NativeVerificationLibrary() {
        // no instances - this class only exposes the guarded native calls
    }

    /// Loads the embedded WRAPS public parameters, if they are not loaded yet, and reports whether
    /// compressed WRAPS proofs can be verified in this process. Serialized against every other native
    /// call. Meant to be called once at start; later calls are cheap and return the same answer.
    ///
    /// @return `true` if WRAPS proof verification is available, `false` if the parameters could not be
    ///     extracted or loaded, in which case every TSS block proof fails verification
    public static boolean warmUp() {
        LOCK.lock();
        try {
            return loadWrapsParameters();
        } finally {
            LOCK.unlock();
        }
    }

    /// Verifies a TSS block signature, serialized against every other native call.
    ///
    /// The signature must be the exact `hintsVerificationKey || hintsSignature || compressedWrapsProof`
    /// layout and the ledger id must be [#LEDGER_ID_LENGTH] bytes; the library rejects anything else
    /// with an `IllegalArgumentException` and returns `false` for a signature that does not verify.
    public static boolean verifyTss(final byte[] ledgerId, final byte[] signature, final byte[] hashToVerify) {
        LOCK.lock();
        try {
            return TSS.verifyTSS(ledgerId, signature, hashToVerify);
        } finally {
            LOCK.unlock();
        }
    }

    /// Asks the library to load its embedded public parameters. The library does this in a static
    /// initializer, so a failure to extract or load them surfaces as an unchecked exception or a
    /// `LinkageError` (`ExceptionInInitializerError` first, `NoClassDefFoundError` afterwards); both
    /// are reported once here with the stack trace instead of surfacing per block.
    ///
    /// The bridges load their native code lazily, in `getInstance()`, while `isProofSupported()`
    /// is static and calls a native method directly: asking for the parameters before an instance
    /// exists fails with an `UnsatisfiedLinkError` that poisons the parameter loader for the rest of
    /// the process. Both instances are therefore obtained first, which also surfaces a native
    /// library that cannot be loaded at all.
    private static boolean loadWrapsParameters() {
        try {
            HintsLibraryBridge.getInstance();
            WRAPSLibraryBridge.getInstance();
            return WRAPSLibraryBridge.isProofSupported();
        } catch (final RuntimeException | LinkageError e) {
            LOGGER.log(
                    WARNING,
                    "The TSS library could not load its WRAPS public parameters (temp directory %s); TSS block proofs cannot be verified"
                            .formatted(System.getProperty("java.io.tmpdir")),
                    e);
            return false;
        }
    }
}
