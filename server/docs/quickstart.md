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

Assuming your working directory is the repo root:

### Build the Server

1. To quickly build the Server sources (without running tests), do the following:
    ```bash
    ./gradlew clean build -x test
    ```

1. To build the Server docker image, do the following:
    ```bash
    ./gradlew createDockerImage
    ```

### Run the Server

1. To start the Server, do the following:
    ```bash
    ./gradlew startDockerContainer
    ```

### Run the Server with Debug

1. To start the Server with debug enabled, do the following:
    ```bash
    ./gradlew startDockerDebugContainer
    ```

1. Attach your remote jvm debugger to port 5005.

### Stop the Server

1. To stop the Server do the following:
    ```bash
    ./gradlew stopDockerContainer
    ```
