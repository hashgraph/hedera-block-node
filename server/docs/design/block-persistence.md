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
      1. [Overview](#overview-2)
      1. [Trie Structure & Algorithm for Block Path Resolution](#trie-structure--algorithm-for-block-path-resolution)
      1. [Specific implementations](#specific-implementations-1)
      1. [Configurable Parameters](#configurable-parameters-1)
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

`BlockNumber` - A value that represents the unique number (identifier) of a
given `Block`. It is an auto-incrementing `long` value, starting from `0` (zero)

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
are the respective `Block`'s `BlockNumber`.

#### Specific implementations

The specific implementations of the defined [abstractions](#abstractions) as
listed above are:

1. `BlockWriter` - `com.hedera.block.server.persistence.storage.write.BlockAsLocalDirWriter`
1. `BlockReader` - `com.hedera.block.server.persistence.storage.read.BlockAsLocalDirReader`
1. `BlockRemover` - `com.hedera.block.server.persistence.storage.remove.BlockAsLocalDirRemover`
1. `BlockPathResolver` - `com.hedera.block.server.persistence.storage.path.BlockAsLocalDirPathResolver`

#### Configurable Parameters

<!-- todo add basePath when defined -->
- `persistence.storage.liveRootPath` - the root path where all `Block`s are
  stored.

#### Purpose

The purpose of this implementation is to provide a simple, local storage. Used
mostly for testing and development purposes.

### Block as Local File (a.k.a. `block-as-local-file`)

#### Overview

This type of storage implementation persists `Block`s to a local filesystem. A
`Block` is persisted as a file containing all the `BlockItem`s that comprise the
given `Block`. The storage has a root. A specific `Block` is stored as a file,
resolved under the storage root by utilizing a `trie` structure. The name of the
file is the respective `Block`'s `BlockNumber`. Optionally, the `Block` can be
compressed before being written to storage. Optionally, the `Block`s can be
archived periodically in batches of configurable size.

#### Trie Structure & Algorithm for Block Path Resolution

The `Block` path resolution is based on a `trie` structure. The `trie` is
constructed as follows:

1. We have a `liveRootPath` to store the `Block`s under.
1. We have an `archiveRootPath` to store the archived `Block`s under.
1. The `BlockNumber` is a `long` which contains 19 digits maximum.
1. The `trie` structure is a folder for each digit in the `BlockNumber`, up to a
   depth of 18.
1. The last digit of the `BlockNumber` is part of the file name itself.
1. The `Block`s are compressed (configurable), `zstd` is used by default.
1. The `Block`s are archived (configurable) in batches, e.g. 1000 `Block`s.

Visual example:
```
(our block files will have a long as a block number, the trie structure will be
a digit per folder for all digits in a long, for brevity, here we showcase only
7 digits, the last digit is part of the block file name itself, we zip every
100 block in a single zip )

BlockArchive/.../1/0/2/3/4.zip/0/1023400.blk.zstd -> zipped because we have configed every 100s to be zipped
BlockLive/.../1/0/2/3/5/0/1023500.blk.zstd -> new block written
BlockLive/.../1/0/2/3/5/0/1023501.blk.zstd -> new block written
...
BlockLive/.../1/0/2/3/5/9/1023599.blk.zstd -> new block written, reaching the 100th mark
BlockArchive/.../1/0/2/3/5.zip/0/1023500.blk.zstd -> block file ...1023500.blk.zstd to ...1023599.blk.zstd are now zipped
BlockLive/.../1/0/2/3/6/0/1023600.blk.zstd -> new block

/.../1/0/2/3/5/9/ + 9 (the last digit of the blk file, block number) = the whole block number ...1023599
```

The algorithm for resolving the path to a `Block` is defined roughly as follows:

1. When writing, initially we only care to write a new `Block` as a file to the
   local filesystem in the correct place, based on the resolved path from the
   `BlockNumber` utilizing the `trie` structure.
1. When writing a new `Block` as file, we need to check if the path to the file
   we want to write already exists, if it does, there is some problem, we should
   handle this case directly in our service.
1. When reading, we should be able to resolve the path to a `Block` as file that
   we want to read, initially attempting to read the file before it would be
   zipped, if not found, we need to search it in the respective zip, else if not
   found, then we have a problem, we need to handle that case inside our
   service.
1. A separate process/thread will periodically go through our `trie` structure
   and will be zipping the `Block` files as per configured amounts as shown
   visually above.

#### Specific implementations

The specific implementations of the defined [abstractions](#abstractions) as
listed above are:

1. `BlockWriter` - `com.hedera.block.server.persistence.storage.write.BlockAsLocalFileWriter`
1. `BlockReader` - `com.hedera.block.server.persistence.storage.read.BlockAsLocalFileReader`
1. `BlockRemover` - `com.hedera.block.server.persistence.storage.remove.BlockAsLocalFileRemover`
1. `BlockPathResolver` - `com.hedera.block.server.persistence.storage.path.BlockAsLocalFilePathResolver`

#### Configurable Parameters

<!-- todo add basePath, archiveBatchSize, compressionMode when defined -->
- `persistence.storage.liveRootPath` - the root path where all `Block`s are
  stored.
- `persistence.storage.archiveRootPath` - the root path where all `Block`s are
  archived.

#### Purpose

The purpose of this implementation is to provide a simple, fast to resolve local
storage. Intended as a production default.

### No Op (a.k.a. `no-op`)