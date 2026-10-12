# Consensus node WRAPS proof vector (hedera-cryptography 3.18.0)

A real genesis chain of trust proof produced by a single node consensus network running the
hedera-cryptography `3.18.0` TSS library, used by `NativeVerificationLibraryTest` to prove that the
Block Node's library build verifies proofs made by a consensus node.

Source: `hedera-node/test-clients/tss-startup-assets/ledgerA-genesis-network.json.gz` in the
hiero-consensus-node repository, branch `27499-adopt-tss-3180` (pull request 27506, head commit
`30ed44cff2` when fetched on 2026-10-12). The file is a gzipped JSON `Network` message; the three
binaries are its base64 fields decoded as is:

|             file             | bytes |                                                                               JSON field                                                                               |
|------------------------------|-------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ledger-id.bin`              | 64    | `ledgerId`                                                                                                                                                             |
| `hints-verification-key.bin` | 1096  | `tssMetadata.activeProofConstruction.targetProof.targetHistory.metadata` (equal to `tssMetadata.activeHintsConstruction.hintsScheme.preprocessedKeys.verificationKey`) |
| `wraps-proof.bin`            | 11368 | `tssMetadata.activeProofConstruction.targetProof.chainOfTrustProof.wrapsProof`                                                                                         |

The first 32 bytes of the ledger id equal `targetProof.targetHistory.addressBookHash`; the last 32
bytes are the library's hash of the hinTS verification key. Regenerate the files from a newer fixture
with the same decoding whenever the consensus node changes its proof format.
