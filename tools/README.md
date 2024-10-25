# Command Line Tools for Block Nodes & Streams

This subproject provides command line tools for working with block stream files and maybe other things in the future. It 
uses [picocli](https://picocli.info) to provide a command line interface which makes it easy to extend and add new 
subcommands or options.

## Running from command line
You can run through gradle with the `tools:run` task. For example, to see the help for the `info` subcommand, you can 
run:

`./gradlew -q tools:run --args="info --help"`

## Subcommands
The following subcommands are available:
- **json**  Converts a binary block stream to JSON
- **info**  Prints info for block files

# JSON Subcommand
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

# Info Subcommand
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