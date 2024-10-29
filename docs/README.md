# Quickstart Guide

This guide will help you get started with the Hedera Block Node.
## Configuration

Refer to the [Configuration](docs/configuration.md) for more information on the configuration options.

## Starting locally:
```bash
./gradlew run
```

In debug mode, you can attach a debugger to the port 5005.
```bash
./gradlew run --debug-jvm
```

Also you can run on docker locally:
```bash
./gradlew startDockerContainer
```

## Running Tests
```bash
./gradlew build
```
