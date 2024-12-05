# Block Persistence

## Table of Contents

1. [Purpose](#purpose)
1. [Terminology](#terminology)
1. [Abstractions](#abstractions)
1. [Goals](#goals)
1. [Overview](#overview)
1. [Implementations](#implementations)
   1. [Block as Local Directory (a.k.a. `block-as-local-dir`)](#block-as-local-directory-aka-block-as-local-dir)
      1. [Overview](#overview-1)
      1. [Specific implementations](#specific-implementations)
      1. [Configurable Parameters](#configurable-parameters)
   1. [Block as Local File (a.k.a. `block-as-local-file`)](#block-as-local-file-aka-block-as-local-file)
   1. [No Op (a.k.a. `no-op`)](#no-op-aka-no-op)

## Purpose

The main objective of the `hedera-block-node` project is to replace the storage
of Consensus Node artifacts (e.g. Blocks) on cloud storage buckets (e.g. GCS and
S3) with a solution managed by the Block Node server. This document aims to
describe the high-level design of how the Block Node persists and retrieves
Blocks and how it handles exception cases when they arise.

## Terminology

`BlockItem` - A `BlockItem` is the primary data structure passed between the
producer, the `hedera-block-node`and consumers. The `BlockItem` description and
protobuf definition are maintained in the `hedera-protobuf`
[project](https://github.com/hashgraph/hedera-protobufs/blob/continue-block-node/documents/api/block/stream/block_item.md).

`Block` - A `Block` is the base element of the block stream at rest. At present,
it consists of an ordered collection of

`BlockItems`. The `Block` description and protobuf definition are maintained in
the `hedera-protobuf`
[project](https://github.com/hashgraph/hedera-protobufs/blob/continue-block-node/documents/api/block/stream/block.md).

`BlockItemUnparsed` - An unparsed, raw version of a `BlockItem`.

`BlockUnparsed` - An unparsed, raw version of a `Block`.

`StorageType` - A value that represents the type of storage used to
persist/retrieve a `Block`.

## Abstractions

`BlockReader` - An interface defining methods used to read a `Block` from
storage. It represents a lower-level component whose implementation is directly
responsible for reading a `Block` from storage.

`BlockWriter` - An interface defining methods used to write `Blocks` to storage.
It represents a lower-level component whose implementation is directly
responsible for writing a `Block` to storage.

`BlockRemover` - An interface defining the methods used to remove a `Block` from
storage. It represents a lower-level component whose implementation is directly
responsible for removing a `Block` from storage.

`BlockPathResolver` - An interface defining methods used to resolve the path to
a `Block` in storage. It represents a lower-level component whose implementation
is directly responsible for resolving the path to a `Block` in storage, based on
`StorageType`.

## Goals

1. `BlockItems` streamed from a producer (e.g. Consensus Node) must be collated
   and persisted as a `Block`. Per the specification, a `Block` is an ordered
   list of `BlockItems`. How the `Block` is persisted is an implementation 
   detail.

1. A `Block` must be efficiently retrieved by block number.

1. Certain aspects of the `Block` persistence implementation must be
   configurable.

## Overview

The design for `Block` persistence is fairly straightforward. `Block` server
objects should use the persistence abstractions to read, write and remove
`Block`s from storage, as well as resolve. `BlockItem`s streamed from a producer
are read off the wire one by one and passed to an implementation of
`BlockWriter`. The `BlockWriter` is responsible for collecting related
`BlockItem`s into a `Block` and persisting the `Block` to storage in a way that
is efficient for retrieval at a later time. The `BlockWriter` is also
responsible for removing a partially written `Block` if an exception occurs
while writing it. For example, if half the `BlockItem`s of a `Block` are written
when an `IOException` occurs, the `BlockWriter`should remove all the
`BlockItem`s of the partially written `Block` and pass the exception up to the
caller. Services requiring one or more `Block`s should leverage a `BlockReader`
implementation. The `BlockReader` should be able to efficiently retrieve a
`Block` by block number.  The `BlockReader` should pass unrecoverable exceptions
when reading a `Block` up to the caller.

## Implementations

### Block as Local Directory (a.k.a. `block-as-local-dir`)

#### Overview

This type of storage implementation persists `Block`s to a local filesystem. A
`Block` is persisted as a directory containing a file for each `BlockItem` that
comprises the given `Block`. The storage has a root path where each directory
under the root path is a given block. The names of the directories (`Block`s)
are the respective `Block`.

#### Specific implementations

The specific implementations of the defined [abstractions](#abstractions) as
listed above are:

1. `BlockWriter` - `com.hedera.block.server.persistence.storage.write.BlockAsLocalDirWriter`
2. `BlockReader` - `com.hedera.block.server.persistence.storage.read.BlockAsLocalDirReader`
3. `BlockRemover` - `com.hedera.block.server.persistence.storage.remove.BlockAsLocalDirRemover`
4. `BlockPathResolver` - `com.hedera.block.server.persistence.storage.path.BlockAsLocalDirPathResolver`

#### Configurable Parameters

- `persistence.storage.liveRootPath` - the root path where all `Block`s are
  stored.

### Block as Local File (a.k.a. `block-as-local-file`)

### No Op (a.k.a. `no-op`)