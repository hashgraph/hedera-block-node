# Command Line Tools for Block Nodes & Streams

## Table of Contents

1. [Overview](#overview)
2. [Running from command line](#running-from-command-line)
3. [Subcommands](#subcommands)
   1. [The `json` Subcommand](#the-json-subcommand)
   2. [The `info` Subcommand](#the-info-subcommand)

## Overview

This subproject provides command line tools for working with block stream files and maybe other things in the future. It
uses [picocli](https://picocli.info) to provide a command line interface which makes it easy to extend and add new
subcommands or options.

## Running from command line

Refer to the [Quickstart](docs/quickstart.md) for a quick guide on how to run the tools CLI.

## Subcommands

The following subcommands are available:
- `json` - Converts a binary block stream to JSON
- `info` - Prints info for block files
- `record2block` - Converts a historical record stream files into blocks
- `fetchRecordsCsv` - Download mirror node record table CSV dump from GCP bucket
- `extractBlockTimes` - Extract block times from mirror node records csv file
- `validateBlockTimes` - Validates a block times file

### The `json` Subcommand

Converts a binary block stream to JSON

`Usage: json [-t] [-ms=<minSizeMb>] [<files>...]`

**Options:**

- `-ms <minSizeMb>` or `--min-size=<minSizeMb>`
   - Filter to only files bigger than this minimum file size in megabytes

- `-t` or `--transactions`
   - expand transactions, this is no longer pure json conversion but is very useful making the
transactions human-readable.

- `<files>...`
   - The block files or directories of block files to convert to JSON

### The `info` Subcommand

Prints info for block files

`Usage: info [-c] [-ms=<minSizeMb>] [-o=<outputFile>] [<files>...]`

**Options:**

- `-c` or `--csv`
   - Enable CSV output mode (default: false)

- `-ms <minSizeMb>` or `--min-size=<minSizeMb>`
   - Filter to only files bigger than this minimum file size in megabytes

- `-o <outputFile>` or `--output-file=<outputFile>`
   - Output to file rather than stdout

- `<files>...`
   - The block files or directories of block files to print info for

### The `record2block` Subcommand

Converts a historical record stream files into blocks. This depends on the `block_times.bin` file being present. It can 
be created by running the other commands `fetchRecordsCsv` and `extractBlockTimes`. It can also be validated by running
the `validateBlockTimes` command.

This command depends on reading data from public requester pays Google Cloud buckets. To do that it needs you to be 
authenticated with the Google Cloud SDK. You can authenticate with `gcloud auth application-default login` or 
`gcloud auth login` see [Google Documentation](https://cloud.google.com/storage/docs/reference/libraries#authentication) 
for more info.

`Usage: record2block [-s 0] [-e 100] [-j] [-c] [--min-node-account-id=3] [--max-node-account-id=34] [-d <dataDir>] [--block-times=<blockTimesFile>] `

**Options:**

- `-s <blockNumber>` or `--start-block=<blockNumber>`
   - The first block number to process
   - Default: 0
- `-e <blockNumber>` or `--end-block=<blockNumber>`
   - The last block number to process
   - Default: 3001
- `-j` or `--json`
   - also output blocks as json, useful for debugging and testing
   - Default: false
- `-c` or `--cache-enabled`
   - Use local cache for downloaded content, saves cloud costs and bandwidth when testing
   - Default: false
- `--min-node-account-id=<minNodeAccountId>`
   - the account id of the first node in the network
   - Default: 3
- `--max-node-account-id=<maxNodeAccountId>`
   - the account id of the last node in the network
   - Default: 34
- `--data-dir=<dataDir>`
   - the data directory for output and temporary files
   - Default: "data"
- `--block-times=<blockTimesFile>`
   - Path to the block times ".bin" file.
   - Default: "data/block_times.bin"

### The `fetchRecordsCsv` Subcommand

Download mirror node record table CSV dump from GCP bucket. The records table on mirror node has a row for every block 
mirror node knows about. The CSV file is huge 11GB+ in November 2024. This data is important for records to blocks 
conversion as we have to make sure the block number assigned for a record file matches what mirror node says as the 
source of truth. 

This command depends on reading data from public requester pays Google Cloud buckets. To do that it needs you to be
authenticated with the Google Cloud SDK. You can authenticate with `gcloud auth application-default login` or
`gcloud auth login` see [Google Documentation](https://cloud.google.com/storage/docs/reference/libraries#authentication)
for more info.

`Usage: fetchRecordsCsv [--record-csv=<recordFilePath>]`

**Options:**

- `--record-csv=<recordFilePath>`
   - Path to the record CSV file.
   - Default: "data/record.csv"

### The `extractBlockTimes` Subcommand

Extract block times from mirror node records csv file. Reads <recordFilePath> and produces <blockTimesFile>. We need to 
convert the mirror node records CSV because it is huge 11GB+ compressed and too large to fit into RAM, and we can not 
random access easily. The only part of the data needed for the records to blocks conversion is the block times. The 
block time being the record file time for a given block. The record file consensus time is used as the file name of the 
record file in the bucket.

The block times file is a binary file of longs, each long is the number of nanoseconds for that block after first block 
time. So first block = 0, second about 5 seconds later etc. The index is the block number, so block 0 is first long, 
block 1 is second block and so on. This file can then be memory mapped and used as fast lookup for block 
number(array offset) into block time, i.e. record file name.

`Usage: extractBlockTimes [--record-csv=<recordFilePath>] [--block-times=<blockTimesFile>]`

**Options:**

- `--record-csv=<recordFilePath>`
   - Path to the record CSV file.
   - Default: "data/record.csv"
- `--block-times=<blockTimesFile>`
   - Path to the block times ".bin" file.
   - Default: "data/block_times.bin"
