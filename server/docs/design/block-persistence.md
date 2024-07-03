# Block Persistence

## Purpose

The main objective of the `hedera-block-node` project is to replace the storage of Consensus Node artifacts (e.g.
Blocks) on cloud storage buckets (e.g. GCS and S3) with a solution managed by the Block Node server. This document aims
to describe the high-level design of how the Block Node persists and retrieves Blocks and how it handles exception cases
when they arise.

---

### Goals

1) BlockItems streamed from a producer (e.g. Consensus Node) must be collated and persisted as a Block.  Per the
   specification, a Block is an ordered list of BlockItems. How the Block is persisted is an implementation detail.
2) A Block must be efficiently retrieved by block number.

---

### Terms

**BlockItem** - A BlockItem is the primary data structure passed between the producer, the `hedera-block-node`
and consumers. The BlockItem description and protobuf definition are maintained in the `hedera-protobuf`
[project](https://github.com/hashgraph/hedera-protobufs/blob/continue-block-node/documents/api/block/stream/block_item.md).

**Block** - A Block is the base element of the block stream at rest. At present, it consists of an ordered collection of
BlockItems. The Block description and protobuf definition are maintained in the `hedera-protobuf`
[project](https://github.com/hashgraph/hedera-protobufs/blob/continue-block-node/documents/api/block/stream/block.md).

---

### Entities

**BlockReader** - An interface defining methods used to read a Block from storage. It represents a lower-level
component whose implementation is directly responsible for reading a Block from storage.

**BlockWriter** - An interface defining methods used to write BlockItems to storage. It represents a lower-level
component whose implementation is directly responsible for writing a BlockItem to storage as a Block.

**BlockRemover** - An interface defining the methods used to remove a Block from storage. It represents a lower-level
component whose implementation is directly responsible for removing a Block from storage.

---

### Design

The design for `Block` persistence is fairly straightforward. Block server objects should use the persistence entity 
interfaces to read, write and remove `Block`s from storage. `BlockItem`s streamed from a producer are read off the wire 
one by one and passed to an implementation of `BlockWriter`. The `BlockWriter` is responsible for collecting related 
`BlockItem`s into a `Block` and persisting the `Block` to storage in a way that is efficient for retrieval at a later 
time. The `BlockWriter` is also responsible for removing a partially written `Block` if an exception occurs while 
writing it. For example, if half the `BlockItem`s of a `Block` are written when an IOException occurs, the `BlockWriter` 
should remove all the `BlockItem`s of the partially written `Block` and pass the exception up to the caller. Services
requiring one or more `Block`s should leverage a `BlockReader` implementation. The `BlockReader` should be able to
efficiently retrieve a `Block` by block number.  The `BlockReader` should pass unrecoverable exceptions when reading
a `Block` up to the caller.
