# Command Line Tools for Block Nodes & Streams

## Table of Contents

1. [Overview](#overview)
1. [Subcommands](#subcommands)
   1. [The `json` Subcommand](#the-json-subcommand)
   1. [The `info` Subcommand](#the-info-subcommand)
1. [Running from command line](#running-from-command-line)

## Overview

This subproject provides command line tools for working with block stream files and maybe other things in the future. It
uses [picocli](https://picocli.info) to provide a command line interface which makes it easy to extend and add new
subcommands or options.

## Subcommands

The following subcommands are available:
- `json` - Converts a binary block stream to JSON
- `info` - Prints info for block files

### The `json` Subcommand

Converts a binary block stream to JSON

`Usage: subcommands json [-t] [-ms=<minSizeMb>] [<files>...]`

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

`Usage: subcommands info [-c] [-ms=<minSizeMb>] [-o=<outputFile>] [<files>...]`

**Options:**

- `-c` or `--csv`
   - Enable CSV output mode (default: false)

- `-ms <minSizeMb>` or `--min-size=<minSizeMb>`
   - Filter to only files bigger than this minimum file size in megabytes

- `-o <outputFile>` or `--output-file=<outputFile>`
   - Output to file rather than stdout

- `<files>...`
   - The block files or directories of block files to print info for

## Running from command line

Refer to the [Quickstart](docs/quickstart.md) for a quick guide on how to run the tools CLI.
