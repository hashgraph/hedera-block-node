# Hedera Block Node

Implementation of the Hedera Block Node, which is responsible for consuming the block streams, maintaining state and exposing additional targeted value adding APIs to the Hedera community.

## Table of Contents

1. [Project Links](#project-links)
1. [Prerequisites](#prerequisites)
1. [Overview of child modules](#overview-of-child-modules)
1. [Getting Started](#getting-started)
1. [Support](#support)
1. [Contributing](#contributing)
1. [Code of Conduct](#code-of-conduct)
1. [Security](#-security)
1. [License](#license)

## Project Links

[![Build Application](https://github.com/hashgraph/hedera-block-node/actions/workflows/build-application.yaml/badge.svg?branch=main)](https://github.com/hashgraph/hedera-block-node/actions/workflows/build-application.yaml)
[![E2E Test Suites](https://github.com/hashgraph/hedera-block-node/actions/workflows/e2e-tests.yaml/badge.svg?branch=main)](https://github.com/hashgraph/hedera-block-node/actions/workflows/e2e-tests.yaml)
[![codecov](https://codecov.io/github/hashgraph/hedera-block-node/graph/badge.svg?token=OF6T6E8V7U)](https://codecov.io/github/hashgraph/hedera-block-node)

[![Latest Version](https://img.shields.io/github/v/tag/hashgraph/hedera-block-node?sort=semver&label=version)](README.md)
[![Made With](https://img.shields.io/badge/made_with-java-blue)](https://github.com/hashgraph/hedera-block-node/)
[![Development Branch](https://img.shields.io/badge/docs-quickstart-green.svg)](docs/README.md)
[![License](https://img.shields.io/badge/license-apache2-blue.svg)](LICENSE)

## Prerequisites

- Java 21 (temurin recommended)
- Gradle (using the wrapper `./gradlew` is highly recommended)
- Docker (recommended for running the projects)
- IntelliJ IDEA (recommended for development)

## Overview of child modules

- `server`: implementation of the block node, which contains the main application and all the necessary code to run the block node.
- `simulator`: A simulator for the block node, which can be used to test the block node in a local environment.
- `common`: Module responsible for holding common literals, utilities and types used by the other modules.
- `suites`: A set of e2e tests that can be used to verify the correctness of the block node.
- `tools`: A set of command line tools for working with block stream files.


## Getting Started

Refer to the [Hedera Block Node Documentation Overview](docs/README.md) for more information about the project, design and guides.

## Support

If you have a question on how to use the product, please see our
[support guide](https://github.com/hashgraph/.github/blob/main/SUPPORT.md).

## Contributing

Contributions are welcome. Please see the
[contributing guide](https://github.com/hashgraph/.github/blob/main/CONTRIBUTING.md)
to see how you can get involved.

## Code of Conduct

This project is governed by the
[Contributor Covenant Code of Conduct](https://github.com/hashgraph/.github/blob/main/CODE_OF_CONDUCT.md). By
participating, you are expected to uphold this code of conduct. Please report unacceptable behavior
to [oss@hedera.com](mailto:oss@hedera.com).

## 🔐 Security

Please do not file a public ticket mentioning the vulnerability. Refer to the security policy defined in the [SECURITY.md](https://github.com/hashgraph/hedera-block-node/blob/main/SECURITY.md).

## License

[Apache License 2.0](LICENSE)
