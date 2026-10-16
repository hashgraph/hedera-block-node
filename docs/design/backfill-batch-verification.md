# Chain-Anchored Batch Verification for Backfill

Status: draft, tracked by #3859.
Baseline: `main` at `5c1994fdf`, plus the hedera-cryptography 3.21.0 move in #3853.

## Purpose

Backfill verifies every block with its own TSS check. On hedera-cryptography 3.21.0 one
`TSS.verifyTSS` call takes about 130 ms (hiero-ledger/hiero-cryptography#753, closed as
expected for Nova WRAPS). `NativeVerificationLibrary` serializes every call behind one
process-wide lock, so the most a node can verify is about 7.7 blocks/s. Observed backfill
runs at 6 to 7 blocks/s. A day of blocks (about 43k) takes about 1.5 h of verification
alone.

This proposal verifies a contiguous batch of backfilled blocks with **one** signature check
plus a hash chain check, instead of one signature check per block.

## Goals

1. Use one TSS verification for each contiguous batch of backfilled blocks. Today it is one
   per block.
2. Keep the security level at the current per-block verification level. The batch check
   relies only on SHA-256 collision resistance, which the TSS signature already depends on.
3. Keep the downstream contract unchanged: one `VerificationNotification` per block, in
   order, with the same `source` and `blockHash`.
4. Keep the worst case no slower than today. Any batch failure falls back to per-block cost.
5. Leave the live publisher path unchanged.

## Why It Is Sound

The block root hash commits to the previous block. In `HashingUtilities.computeFinalBlockHash`,
leaf 1 of the 16-leaf root tree is `BlockFooter.previousBlockRootHash`.

For a batch `[s .. e]`:

1. Hash every block and compute `R(s) .. R(e)`.
2. **Link check.** For each `i` in `s+1 .. e`, require `footer(i).previousBlockRootHash == R(i-1)`.
3. **Anchor check.** Verify the proof of block `e` (TSS, or a state proof) against `R(e)`.

If step 3 passes, the network signed `R(e)`. `R(e)` commits to `footer(e).previousBlockRootHash`,
and step 2 shows that value equals `R(e-1)`. So `R(e-1)` is the hash the network chained from.
Applied inductively down to `s`, every block in the batch is authenticated. To forge block `k`,
an attacker must find a second preimage for `R(k)` under SHA-256.

The anchor must be the **last** block of the batch. The chain points backward, so a verified
first block says nothing about the blocks after it.

## Concerns Raised in Review

**"Trusting blocks based on a later proof makes it too easy to sneak in a flawed block."**
It does not. Changing any byte of a block inside the batch changes its root hash. Then the next
block's `previousBlockRootHash` no longer matches, and the block is rejected. Forging a block
requires a SHA-256 second preimage. Every TSS check already relies on that assumption, since
TSS signs only a 32-byte hash. The protocol already accepts this trust model for
`block_state_proof`.

**"…or a proof-free block."**
Every block must still carry a proof, or it fails `MISSING_MANDATORY_ITEM` as today. Every
intermediate proof gets a shape check. The remaining gap, a well-formed proof with forged
signature bytes, is described in
[Proofs of intermediate blocks](#proofs-of-intermediate-blocks-decision-shape-check).

**"We need to verify every block proof for every block."**
Per-block proof verification gives no extra assurance about a block's contents beyond the
chain. Its only additional guarantee is that the stored proof bytes are valid. Recovering that
guarantee costs the full 130 ms per block that this design removes.

**"Verification is already parallel, one thread per block."**
Hashing is parallel: each block has its own session on a virtual thread. TSS is not. Every
`TSS.verifyTSS` call goes through the process-wide lock in `NativeVerificationLibrary`, because
concurrent calls corrupted native state on 3.13.0. The expensive step runs one call at a time,
and that sets the ceiling of about 7.7 blocks/s. Parallel TSS depends on the thread-safety
question in hiero-cryptography#753. If it is safe, it combines with batching.

**"Holding a whole batch in memory until the last block verifies is a risk."**
Holding the whole batch is not new. Backfill already fetches 10 to 20 blocks per request
(`fetchBatchSize`). It holds all of them until they are verified and persisted:
`fetchBlocksFromNode` returns the full chunk, and every block is sent to verification back to
back. What changes is the batch size. With batches of 100, the worst case would be 100 blocks ×
20 MB, about 2 GB. Each batch is therefore also capped by total bytes, not only by block count
(see [Memory bound](#memory-bound)).

**"Bulk load from a trusted, signed package is the fastest option."**
Agreed for raw load speed. It trusts the packager. Batch verification trusts only the network's
signature and SHA-256, at a comparable verification speed (see
[Expected Throughput](#expected-throughput)). The two combine: a bulk package can be verified
the same way, with hashing offline and one anchor every N blocks.

## Current State (facts from the code)

- **No cross-block hash check today, and none is needed per block.** `previousBlockRootHash`
  is only hashed into the root (`BlockHasher.finalHashingResult` :414-477). Each block carries
  its own signature over a root that already commits to its predecessor, so per-block
  verification does not need the previous block. That is why sessions can verify out of order,
  which is common during backfill. The link check in this design is needed only inside a batch,
  where all blocks are in hand, because the intermediate blocks have no verified signature of
  their own.
- **Block proofs are not covered by the root hash.** `BLOCK_PROOF` items are collected and
  never hashed (`BlockHasher.processItem`).
- **Backfill is batched only at fetch time.** `BackfillRunner` fetches `fetchBatchSize`
  (default 10, max 1,024) blocks. It then sends one `BackfilledBlockNotification` per block,
  awaits persistence per block (`perBlockProcessingTimeout` 1,000 ms), and sleeps
  `delayBetweenBatches` (1,000 ms) before the next chunk.
- **Hashing runs in parallel, TSS does not.** Each block has its own `CompletableVerificationSession`
  on a virtual-thread executor. All `verifyTss` calls go through `NativeVerificationLibrary.LOCK`.
- **`StateProofVerifier`** already does "one signature covers another root". It only proves the
  current block's own root, inside that block's session.

## Design

### Option A (recommended): explicit batch notification

The batch boundary is chosen by backfill, which already holds the batch, and is passed to
verification explicitly.

1. **SPI.** Add `BackfilledBatchNotification(long startBlockNumber, List<BlockUnparsed> blocks)`.
   The blocks must be contiguous. Backfill sends one notification per chunk instead of N.
   The single-block `BackfilledBlockNotification` stays for block 0, retries, and single-block gaps.
2. **Verification plugin.** Add a `BatchVerificationSession` with these stages:
   - **Hash.** Hash every block in parallel with the existing `BlockHasher`, giving
     `HashingResult` per block.
   - **Link.** Check the footers from `s+1` to `e`. Block `s` is not linked to `s-1`: batches
     are independent, can verify out of order, and do not depend on each other.
   - **Segment.** A link break at `k` splits the batch into `[s .. k-1]` and `[k .. e]`.
     Each segment is anchored on its own last block, so it costs one TSS check per segment.
   - **Anchor.** Run the existing `BlockVerifier` on the last block of each segment. TSS,
     state proof and RSA all work unchanged.
   - **Emit.** Send one success `VerificationNotification` per block through
     `ResultOrderingManager`, in order. Downstream sees exactly what it sees today.
3. **Anchor failure.** Report the anchor block as `BAD_BLOCK_PROOF`. Re-anchor the rest of the
   segment on block `e-1`, or simply fall back to per-block sessions. The worst case is then
   today's cost. Blocks above a failed anchor are never reported as verified.
4. **Backfill.** Await persistence with a batch-scoped deadline, not a per-block timeout:
   the first block completes only when the whole batch is verified. Raise the effective
   `fetchBatchSize` (for example 100). Revisit `delayBetweenBatches`, which caps throughput
   at about B blocks per second on its own, whatever the verify cost.

### Option B: deferred anchoring inside per-block sessions

Keep the per-block notifications. A backfill session whose root hash equals the
`previousBlockRootHash` of a verified block `N+1` is marked verified. Sessions park until
an anchor arrives. This needs no SPI change. It does need lookahead state, anchor timeouts
and interaction with session eviction (`activeSessionsBufferSize`). It is harder to reason
about, and is not recommended.

### Proofs of intermediate blocks (decision: shape check)

Because proofs are outside the root hash, a batch-verified intermediate block's
`BLOCK_PROOF` bytes are **not authenticated**. A malicious peer could keep the block contents
and swap in garbage proofs. The node would then persist and serve blocks whose own proofs
fail for downstream consumers that verify per block (Mirror Node, other BNs,
`BlockContentProof`).

**Decision:** every intermediate proof gets a cheap shape check. It must have:
- the expected oneof;
- the expected signature length (13,712 B on the current format);
- a ledger id / VK prefix equal to the anchor's.

A block whose proof fails the shape check fails with `BAD_BLOCK_PROOF`, and the batch splits
around it.

This catches malformed or substituted proofs from a broken peer. It does not catch a
well-formed proof with forged signature bytes. That gap is accepted, and documented in
`docs/design/block-verification.md`.

The impact of the gap is bounded:
- The block's **contents** are still authenticated through the chain. A forged proof cannot
make any consumer accept bad data.
- The worst case is that a correct block fails a downstream per-block proof check. That is a
visible failure, not silent corruption.
- It requires a malicious peer to serve the batch.

#### How this differs from state proofs

Batch verification and `block_state_proof` share the same trust model: a signature on a later
root authenticates an earlier block's contents through hashes. They differ in what the node
stores:

|                      |                                          State proof                                           |                 Batch, intermediate block                 |
|----------------------|------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| Proof in the block   | The state proof itself: Merkle paths up to a later signed root, plus that root's TSS signature | The block's own TSS signature                             |
| Verified by the node | Fully, by `StateProofVerifier`                                                                 | Shape check only                                          |
| Stored proof valid   | Yes, always                                                                                    | Yes for honest peers; not guaranteed for a malicious peer |

The gap is therefore specific to batching. A state proof block carries the very proof that
was checked. A batch-verified intermediate block carries a proof that was not checked.

Considered and rejected for now: a background audit that fully verifies intermediate proofs
at low priority. It would spend the same TSS cost that batching saves.

### Constraints

- Batches never include block 0, which is already sent alone for TSS bootstrap. A batch must not
  span a ledger id or TSS-data change.
- The publisher (live) path is not affected: one block per about 2 s does not need batching.
- `allSourcesRequireOrdering=true` (the Tier 1 default) is preserved, because emission is ordered.
- Memory: see [Memory bound](#memory-bound).

### Memory bound

A batch holds all of its blocks in memory until its anchor verifies. A block count alone is
not a safe bound, because block size varies widely. For example, 100 blocks of 31.6 MB
(fixture block 322) would hold over 3 GB in memory. A batch is therefore capped by **total
serialized bytes** as well as by block count:

- **Building batches.** Backfill adds blocks in order to a verification batch until either:
  - the next block would push the batch over `backfill.maxBatchBytes`, or
  - the batch reaches `fetchBatchSize` blocks.
- **Large chunks.** A fetched chunk above the cap is split into several verification batches.
  Each one is anchored on its own last block, so it costs one TSS check per batch.
- **Oversized blocks.** A single block larger than the cap becomes a batch of one, verified per
  block as today.
- **Total memory.** Each backfill scheduler (historical and live tail) keeps one batch in flight
  at a time. So batch memory stays at about 2 × `maxBatchBytes`, plus the hashing working set.
- **Fetch side.** The bound sits next to the existing ones: `maxIncomingBufferSize` (100 MiB)
  and `maxProtobufMessageSizeBytes` (about 131 MB per message).
- **Throughput.** With small blocks the byte cap is never hit. With large blocks, hashing time
  already dominates the one TSS call, so smaller batches cost little.

## Diagram

```mermaid
sequenceDiagram
    participant BF as Backfill
    participant BV as Verification
    participant P as Persistence
    BF->>BF: fetch blocks s..e from peer
    BF->>BV: BackfilledBatchNotification(s, [s..e])
    par hash in parallel
        BV->>BV: R(s) .. R(e)
    end
    BV->>BV: link check footer(i).prev == R(i-1)
    BV->>BV: TSS verify proof(e) against R(e) (one locked call)
    loop i = s..e, in order
        BV->>P: VerificationNotification(i, success, R(i))
    end
    P-->>BF: PersistedNotification(i)
```

## Expected Throughput

The TSS cost per block becomes `130 ms / B`. For example, B = 100 gives 1.3 ms per block,
so throughput is then bounded by fetch, hashing, and persistence, not by the native lock.

### Measured hashing cost

The real `BlockHasher` was timed on the fixture blocks (Apple M1 Max, 10 cores, JDK 25.0.2).
Each block was warmed up first, then timed on one thread (median) and on a fixed pool of
10 threads. Hashing is pure SHA-256 in Java and does not touch the native TSS library.

|          Block          |  Size   | One thread, median |       One thread        |       10 threads        |
|-------------------------|---------|--------------------|-------------------------|-------------------------|
| WRAPS block 3 (small)   | 14 KB   | 0.67 ms            | ~1,500 blocks/s         | ~7,700 blocks/s         |
| WRAPS block 0 (genesis) | 365 KB  | 1.36 ms            | ~735 blocks/s           | ~720 blocks/s (1)       |
| WRAPS block 322 (large) | 31.6 MB | 37.7 ms            | ~27 blocks/s (840 MB/s) | ~77 blocks/s (2.4 GB/s) |

(1) Block 0 runs the one-off TSS bootstrap path and does not scale. It is always sent alone.

For comparison, one `TSS.verifyTSS` call takes about 130 ms, one call at a time. A 31.6 MB
block hashes about 3.4 times faster than one signature check, and a small block about 200
times faster.

Estimated verification cost per batch of B = 100 small blocks:
- about 13 ms of parallel hashing, plus one 130 ms TSS call;
- so about 143 ms per batch, or **about 700 blocks/s**;
- today's ceiling is about 7.7 blocks/s.

At that rate, fetch and persistence limit throughput before verification does.

These are test-network fixtures, not mainnet captures. The MB/s figures scale to any
mainnet block size. Re-measure on mainnet captures before setting a target.

## Configuration

|                Property                 |         Default          |                       Notes                        |
|-----------------------------------------|--------------------------|----------------------------------------------------|
| `verification.batchVerificationEnabled` | `true`                   | Kill switch, falls back to per-block               |
| `backfill.fetchBatchSize`               | revisit (10 today)       | Directly sets the TSS amortization factor          |
| `backfill.delayBetweenBatches`          | revisit (1,000 ms today) | Currently a throughput cap on its own              |
| `backfill.batchProcessingTimeout`       | new                      | Replaces per-block awaiting for batches            |
| `backfill.maxBatchBytes`                | new, 256 MiB             | Upper bound on the bytes of one verification batch |

## Metrics

- `verification_batch_total{result}`, `verification_batch_size` and `verification_batch_bytes` (histograms)
- `verification_batch_link_breaks_total`
- `verification_tss_lock_wait_time`, which is missing today, and is useful regardless of this change
- `verification_proof_shape_check_failures_total`

## Exceptions

- Link break: split into segments. This is not a failure by itself.
- Anchor `BAD_BLOCK_PROOF`: fail only the anchor, then re-anchor or fall back per block.
- Malformed block inside a batch (parse error, missing footer): fail that block, then split
  the batch around it.

## Acceptance Tests

1. A valid batch of N blocks makes exactly one `verifyTss` call and sends N in-order success notifications.
2. Tampered contents in block `k < e` cause a link break at `k+1`. Block `k` is never reported verified.
3. A tampered anchor signature sends no success for any block in the segment.
4. A garbage proof on an intermediate block (wrong type, wrong length, or a mismatched prefix)
   fails the shape check, and that block is not reported verified.
5. A batch with a link break costs one TSS call per segment.
6. Per-block fallback behaves exactly like today when `batchVerificationEnabled=false`.
7. A backfill throughput benchmark shows a gain over the 7.7 blocks/s ceiling on 3.21.0.
8. No verification batch exceeds `maxBatchBytes`. A chunk above the cap is split into several
   batches, and a single block larger than the cap is verified per block.

## Related / Complementary

- **Concurrent TSS.** Question 2 in hiero-cryptography#753 (is `verifyTSS` thread-safe on
  3.21.0?) was not answered when the issue closed. If it is safe, dropping the lock scales
  with cores and combines with batching.
