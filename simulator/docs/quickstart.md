# Quickstart of the Simulator

## Table of Contents

1. [Configuration](#configuration)
1. [Running locally](#running-locally)
   1. [Build the Simulator](#build-the-simulator)
   1. [Run the Server first](#run-the-server-first)
   1. [Run the Simulator](#run-the-simulator)
   1. [Run the Simulator with Debug](#run-the-simulator-with-debug)

## Configuration

Refer to the [Configuration](configuration.md) for configuration options.

## Running locally:

- Simulator subproject qualifier:`:simulator`
- Assuming your working directory is the repo root

> **NOTE:** one may use the `-p` flag for `./gradlew` in order to avoid
> specifying the target subproject repeatedly on each task when running
> multiple tasks. When running only a single task, however, it is
> recommended to use the project qualifier (i.e. `:simulator:`) for
> both simplicity and clarity.

### Build the Simulator

> **NOTE:** if you have not done so already, it is
> generally recommended to build the entire repo first:
> ```bash
> ./gradlew clean build -x test
> ```

1. To quickly build the Simulator sources (without running tests), do the following:
   ```bash
   ./gradlew -p simulator clean build -x test
   ```

### Run the Server first

Usually, you would want to run the [Server](../../server/README.md) first, refer to the
[Quickstart of the Server](../../server/docs/quickstart.md) for a quick guide on how to
get started with the application.

### Run the Simulator

1. To start the Simulator, do the following:
   ```bash
   ./gradlew :simulator:run
   ```

### Run the Simulator with Debug

1. To start the Simulator with debug enabled, do the following:
   ```bash
   ./gradlew :simulator:run --debug-jvm
   ```

1. Attach your remote jvm debugger to port 5005.