# TSS Block Proof Verification

## Purpose

Cryptographically confirms that blocks were signed by a threshold of network stake using TSS.
For the broader block verification design, see
[`docs/design/block-verification.md`](block-verification.md).

## Terms

**Ledger ID** (network ID): the identity of a network's chain of trust and the only input a
Block Node needs, besides the block itself, to verify a TSS block proof. It is 64 bytes: the hash
of the genesis TSS address book followed by the hash of the hinTS verification key that address
book first adopted, as computed by the TSS library (`WRAPSLibraryBridge.computeNetworkID`).
Published by the consensus network in `LedgerIdPublicationTransactionBody.ledger_id`, normally
in block 0.

**TssSignedBlockProof**: a `BlockProof` whose `block_signature` is the concatenation of three
parts produced by the hedera-cryptography TSS library:

|           part            | bytes  |                            meaning                            |
|---------------------------|--------|---------------------------------------------------------------|
| hinTS verification key    | 1,096  | the key the signing roster's hinTS signatures verify under    |
| hinTS aggregate signature | 1,248  | the threshold signature over the block root hash              |
| compressed WRAPS proof    | 11,368 | the chain of trust proof placing that key under the ledger id |

The three parts total 13,712 bytes and this is the only layout the library accepts; every block,
including block 0, carries a WRAPS proof. The library verifies all three parts with
`TSS.verifyTSS(ledgerId, blockSignature, blockRootHash)`: the WRAPS proof against the genesis
address book hash half of the ledger id and the embedded hinTS verification key, then the hinTS
signature against that key and the hash.

**WRAPS public parameters**: the parameters the WRAPS proof system is verified with are
embedded in the TSS library jar. There is no verification key to configure, download or publish.

## Design

Production code touches the library in one place, `NativeVerificationLibrary`, a gateway that
serializes every native call behind a process-wide lock (the library's singletons are not thread
safe for concurrent verification) and exposes:

- `warmUp()`: asks the library to load its embedded WRAPS parameters. The library extracts them
  (about 64 MB) into the directory named by the `java.io.tmpdir` system property on first use
  and loads them natively, which takes a few seconds and holds roughly 200 MB off heap for the
  life of the process. `VerificationServicePlugin.start()` calls this once (unless
  `verification.tssWarmUpEnabled` is false) so the first block does not pay for it and a temp
  directory the library cannot write to is reported at start (an INFO line when ready, a WARNING
  otherwise; TSS block proofs then fail verification until the process is restarted). The
  bridges load their native code lazily in `getInstance()`, so the gateway obtains both instances
  before asking for the parameters.
- `verifyTss(ledgerId, signature, hash)`: the composite verification above.
- `LEDGER_ID_LENGTH` (64): the ledger id length the library accepts.

`TSSVerifier` runs the verification for each block that carries a `signed_block_proof` (and
`StateProofVerifier` for the signed proof inside a block state proof), using the ledger id of the
current `TssData` held by `VerificationDataProvider`. The library throws
`IllegalArgumentException` for a malformed signature or ledger id and `IllegalStateException`
when it is not initialized; both are reported as `BAD_BLOCK_PROOF`. No TSS data at all is
reported as `MISSING_VERIFICATION_DATA`.

### TSS data

`TssData` (see `node_service.proto`) carries the ledger id, the block it is valid from and, for
information only, the roster that published it. The `wraps_verification_key` field is kept for
wire compatibility and is always empty. `VerificationDataProvider.updateTssData` installs a
datum when it differs from the current one, its ledger id is 64 bytes, and it is not valid from
an earlier block than the current datum; a datum with a ledger id of another length comes from a
bootstrap file or a peer that predates the current TSS library and is ignored with a WARNING, so
block 0 of the stream can still provision the current one. A roster is not required.

`TssData` reaches the provider from three sources, all of them through the
`ApplicationStateFacility` and its `TssDataNotification`:

1. **Block 0 of the stream.** `BlockHasher` scans the signed transactions of block 0 for a
   `LedgerIdPublicationTransactionBody` while no TSS data is held, and `VerificationHelper`
   maps it to `TssData` (ledger id, node contributions as the roster, valid from block 0). The
   publication's `history_proof_verification_key` is no longer set by consensus nodes and is not
   read.
2. **Persisted file.** `BlockNodeApp` loads `app.state.tssBootstrapFilePath` (default
   `/opt/hiero/block-node/application-state/tss-bootstrap-roster.json`, `TssData` as JSON)
   at start and rewrites it whenever the data changes.
3. **Peer Block Nodes.** The `roster-bootstrap-tss` plugin queries the peers listed in its
   block node sources file and installs every answer whose ledger id is 64 bytes.

For nodes joining an existing network where block 0 will never be replayed, the bootstrap file
can be placed manually into the state volume; the `blocks validate` tool of the Block Stream
Tool writes it from a stream that contains the publication.

### Migration across TSS library changes

A TSS library change that alters the ledger id derivation or the proof format (such as the move
to hedera-cryptography 3.18.0) changes the ledger id of every TSS network and makes blocks of the
previous format unverifiable. A Block Node upgraded across such a change keeps ignoring its old
bootstrap file and waits for block 0 of the new format, or is given the new bootstrap file by
its operator; blocks persisted in the old format do not re-verify.

## Configuration

|                  Property                   |                               Default                               |                                                                         Description                                                                          |
|---------------------------------------------|---------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `app.state.tssBootstrapFilePath`            | `/opt/hiero/block-node/application-state/tss-bootstrap-roster.json` | Where `TssData` is persisted and loaded                                                                                                                      |
| `roster.bootstrap.tss.blockNodeSourcesPath` | empty                                                               | Peer Block Nodes to query for `TssData`                                                                                                                      |
| `verification.tssWarmUpEnabled`             | `true`                                                              | Load the WRAPS parameters at plugin start; disable only for memory constrained deployments that never receive TSS block proofs (the nano chart profile does) |

The library reads no configuration: the WRAPS parameters are embedded in its jar and extracted
into `java.io.tmpdir`.

## Metrics

`verification_proof_total{proof_type="tss", result=...}` counts TSS verification successes and
failures (see `docs/block-node/metrics.md`).
