# Quickstart of the Server

## Table of Contents

1. [Configuration](#configuration)
1. [Running locally](#running-locally)
   1. [Build the Server](#build-the-server)
   1. [Run the Server](#run-the-server)
   1. [Run the Server with Debug](#run-the-server-with-debug)
   1. [Stop the Server](#stop-the-server)

## Configuration

Refer to the [Configuration](configuration.md) for configuration options.

## Running locally:

- Server subproject qualifier: `:server`
- Assuming your working directory is the repo root

> **NOTE:** using the `-p` flag for `./gradlew` so not to specify the target subproject
> on each task when running multiple tasks, but using the qualifier `:server:` when
> we have only one task to run.

### Build the Server

> **NOTE:** if you have not done so already, it is
> generally recommended to build the entire repo first:
> ```bash
> ./gradlew clean build -x test
> ```

1. To quickly build the Server sources (without running tests), do the following:
    ```bash
    ./gradlew -p server clean build -x test
    ```

1. To build the Server docker image, do the following:
    ```bash
    ./gradlew :server:createDockerImage
    ```

### Run the Server

1. To start the Server, do the following:
    ```bash
    ./gradlew :server:startDockerContainer
    ```

### Run the Server with Debug

1. To start the Server with debug enabled, do the following:
    ```bash
    ./gradlew :server:startDockerDebugContainer
    ```

1. Attach your remote jvm debugger to port 5005.

### Stop the Server

1. To stop the Server do the following:
    ```bash
    ./gradlew :server:stopDockerContainer
    ```
