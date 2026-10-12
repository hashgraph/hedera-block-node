# Harness blocks 0 to 2 in the pre-3.17 TSS proof layout

Generated on 2026-10-12 from commit `93a373dd4` (hedera-cryptography 3.13.0) with:

```
./gradlew :block-verification:generateHarnessBlocks -PoutputDir=<folder> -Pcount=3 [-Palgorithm=SHA2_384] --no-daemon
```

Block 0 carries a `LedgerIdPublication` with a 32 byte ledger id, a 1768 byte WRAPS verification key and a
192 byte Schnorr public key; every block proof is the 2920 byte `hintsVk || hintsSig || aggregateSchnorrSignature`
layout that Block Node releases up to 0.45.x verify. `sha2-256/` is hashed with SHA-256 (releases 0.45.x),
`sha2-384/` with SHA-384 (releases before 0.45.0).

Purpose: the node operator lifecycle workflow (`.github/workflows/e2e-node-operator.yaml`) installs the latest
GA Block Node and pushes blocks 0 to 2 to it before upgrading to the snapshot build. Since the snapshot moved
to hedera-cryptography 3.18.0 (64 byte ledger id, WRAPS proof on every block) its harness can no longer
produce the layout an older release verifies, so the workflow pushes the committed set matching the base
release's hash algorithm in the install phase whenever the base release predates 0.46.0, and writes the
snapshot's own TSS bootstrap file into the state volume before the upgrade. Delete this folder and the
`BASE_TSS_LIB_ERA` gate in the workflow once the latest GA release ships 3.18.0.

|        file         | gzipped size |                              sha256                              |
|---------------------|--------------|------------------------------------------------------------------|
| `sha2-256/0.blk.gz` | 4163         | 2db09e629a9e3a28b1026aa8847e91cf94478f95fa0b66243139f489a2f65821 |
| `sha2-256/1.blk.gz` | 2178         | 1661c75fd351749a3a6cf9479b8578bb3d28cc183082c81833b47baa2d24f0ce |
| `sha2-256/2.blk.gz` | 2212         | e9da8dd3f94c6f6bb990547a761dcb02c6d0410d8087be166e1d954b3b20f949 |
| `sha2-384/0.blk.gz` | 4180         | 08b64870bccb391abed9c5988cd1ecf70adf3c0c24b7b36d790ca58d1519a2d5 |
| `sha2-384/1.blk.gz` | 2217         | 44522c5523d35338c41396e64a68905eca0ecef327811c26796783fa653fcce9 |
| `sha2-384/2.blk.gz` | 2263         | 028597fff4d2f601b5b0f7fa6ebd331df775de9a991c45016309cf4ae543715e |
