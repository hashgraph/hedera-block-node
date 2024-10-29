[![Build Application](https://github.com/hashgraph/hedera-block-node/actions/workflows/build-application.yaml/badge.svg?branch=main)](https://github.com/hashgraph/hedera-block-node/actions/workflows/build-application.yaml)
[![E2E Test Suites](https://github.com/hashgraph/hedera-block-node/actions/workflows/e2e-tests.yaml/badge.svg?branch=main)](https://github.com/hashgraph/hedera-block-node/actions/workflows/e2e-tests.yaml)
[![codecov](https://codecov.io/github/hashgraph/hedera-block-node/graph/badge.svg?token=OF6T6E8V7U)](https://codecov.io/github/hashgraph/hedera-block-node)

[![Latest Version](https://img.shields.io/github/v/tag/hashgraph/hedera-block-node?sort=semver&label=version)](README.md)
[![Made With](https://img.shields.io/badge/made_with-java-blue)](https://github.com/hashgraph/hedera-block-node/)
[![Development Branch](https://img.shields.io/badge/docs-quickstart-green.svg)](docs/gradle-quickstart.md)
[![License](https://img.shields.io/badge/license-apache2-blue.svg)](LICENSE)

# Hedera Block Node
Implementation of the Hedera Block Node, which is responsible for consuming the block streams, maintaining state and exposing additional targeted value adding APIs to the Hedera community.

## Overview of child modules

- `server`: implementation of the block node, which contains the main application and all the necessary code to run the block node.
- `simulator`: A simulator for the block node, which can be used to test the block node in a local environment.
- `common`: Module responsible for holding common literals, utilities and types used by the other modules.
- `suites`: A set of e2e tests that can be used to verify the correctness of the block node.

## Getting Started

Refer to the [Quickstart Guide](docs/README.md) for how to work with this project.

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

# 🔐 Security

Please do not file a public ticket mentioning the vulnerability. Refer to the security policy defined in the [SECURITY.md](https://github.com/hashgraph/hedera-block-node/blob/main/SECURITY.md).

## License

[Apache License 2.0](LICENSE)
