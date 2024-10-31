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
1. **generator:** responsible for generating blocks, exposes a single interface `BlockStreamManager` and several implementations
   1. BlockAsDirBlockStreamManager: generates blocks from a directory, each folder is a block, and block-items are single 'blk' or 'blk.gz' files.
   2. BlockAsFileBlockStreamManager: generates blocks from a single file, each file is a block, used to the format of the CN recordings. (since it loads blocks on memory it can stream really fast, really useful for simple streaming tests)
   3. BlockAsFileLargeDataSets: similar to BlockAsFileBLockStreamManager, but designed to work with GB folders with thousands of big blocks (since it has a high size block and volume of blocks, is useful for performace, load and stress testing)
2. **grpc:** responsible for the communication with the Block-Node, currently only has 1 interface `PublishStreamGrpcClient` and 1 Implementation, however also exposes a `PublishStreamObserver'

## Configuration

Refer to the [Configuration](docs/configuration.md) for configuration options.

## Building the Project

To build the project, run the following command:

```sh
./gradlew :simulator:build
```

## Running the Project

Usually you will want to run a Block-Node server before the simulator, for that you can use the following commnad:

```sh
    ./gradlew :server:run
```
However we recommend running the block-node server as a docker container:
```sh
./gradlew :server:build :server:createDockerImage :server:startDockerContainer
```

Once the project is built, you can run it using the following command:

```sh
./gradlew :simulator:run
```
