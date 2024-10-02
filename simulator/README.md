# Block Stream Simulator

## Overview

The Block Stream Simulator is designed to simulate block streaming for Hedera Hashgraph.
It uses various configuration sources and dependency injection to manage its components.

## Prerequisites

- Java 21
- Gradle
- IntelliJ IDEA (recommended for development)

## Project Design Structure

Uses Dagger2 for dependency injection, the project has a modular structure and divides the Dagger dependencies into modules, but all modules used can be found at the root Injection Module:
```plaintext
src/java/com/hedera/block/simulator/BlockStreamSimulatorInjectionModule.java
```
Entry point for the project is `BlockStreamSimulator.java`, in wich the main method is located and has 2 functions:
1. Create/Load the Application Configuration, it does this using Hedera Platform Configuration API.
2. Create a DaggerComponent and instantiate the BlockStreamSimulatorApp class using the DaggerComponent and it registered dependencies.
3. Start the BlockStreamSimulatorApp, contains the orchestration of the different parts of the simulation using generic interfaces and handles the rate of streaming and the exit conditions.

The BlockStreamSimulatorApp consumes other services that are injected using DaggerComponent, these are:
1. generator: responsible for generating blocks, exposes a single interface `BlockStreamManager` and several implementations
    a. BlockAsDirBlockStreamManager: generates blocks from a directory, each folder is a block, and block-items are single 'blk' or 'blk.gz' files.
    b. BlockAsFileBlockStreamManager: generates blocks from a single file, each file is a block, used to the format of the CN recordings. (since it loads blocks on memory it can stream really fast, really useful for simple streaming tests)
    c. BlockAsFileLargeDataSets: similar to BlockAsFileBLockStreamManager, but designed to work with GB folders with thousands of big blocks (since it has a high size block and volume of blocks, is useful for performace, load and stress testing)
2. grpc: responsible for the communication with the Block-Node, currently only has 1 interface `PublishStreamGrpcClient` and 1 Implementation, however also exposes a `PublishStreamObserver'

## Configuration

There are 2 configuration sets:
1. BlockStreamConfig: contains the configuration for the Block Stream Simulator logic and the generation module.
2. GrpcConfig: contains the configuration for the gRPC communication with the Block-Node.

### BlockStreamConfig
Uses the prefix `blockStream` so all properties should start with `blockStream.`

| Key                      | Description                                                                                                                                                                | Default Value                   |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| `generationMode`         | The desired generation Mode to use, it can only be `DIR` or `AD_HOC`                                                                                                       | `DIR`                           |
| `folderRootPath`         | If the generationMode is DIR this will be used as the source of the recording to stream to the Block-Node                                                                  | ``                              |
| `delayBetweenBlockItems` | The delay between each block item in nanoseconds                                                                                                                           | `1_500_000`                     |
| `managerImplementation`  | The desired implementation of the BlockStreamManager to use, it can only be `BlockAsDirBlockStreamManager`, `BlockAsFileBlockStreamManager` or `BlockAsFileLargeDataSets`  | `BlockAsFileBlockStreamManager` |
| `maxBlockItemsToStream`  | exit condition for the simulator and the circular implementations such as `BlockAsDir` or `BlockAsFile` implementations                                                    | `10_000`                        |
| `paddedLength`           | on the `BlockAsFileLargeDataSets` implementation, the length of the padded left zeroes `000001.blk.gz`                                                                     | 36                              |
| `fileExtension`          | on the `BlockAsFileLargeDataSets` implementation, the extension of the files to be streamed                                                                                | `.blk.gz`                       |

### GrpcConfig
Uses the prefix `grpc` so all properties should start with `grpc.`

| Key             | Description                | Default Value |
|-----------------|----------------------------|---------------|
| `serverAddress` | The host of the Block-Node | `localhost`   |
| `port`          | The port of the Block-Node | `8080`        |

## Building the Project

To build the project, run the following command:

```sh
./gradlew build
