# hedera-block-node
The block node is a new and unique node designed to consume the blocks streams, maintain state and expose additional targeted value adding APIs to the Hedera community.
More details to come as this is work in progress

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

## License

[Apache License 2.0](LICENSE)

# 🔐 Security

Please do not file a public ticket mentioning the vulnerability. Refer to the security policy defined in the [SECURITY.md](https://github.com/hashgraph/hedera-sourcify/blob/main/SECURITY.md).

---

# Running Locally

1) Create a local temp directory.  For example, use `mktemp -d -t block-stream-temp-dir` to create a directory
2) Configuration variables
```
export BLOCKNODE_STORAGE_ROOT_PATH=<path to the temp directory> # You can add this to your .zshrc, etc
```
3) Optional Configuration variables
```
export BLOCKNODE_SERVER_CONSUMER_TIMEOUT_THRESHOLD="<NumberInMiliseconds>" #Default is 1500
```

3) ./gradlew run  # ./gradlew run --debug-jvm to run in debug mode

# Running Tests
1) ./gradlew build


### Code Coverage
[![codecov](https://codecov.io/github/hashgraph/hedera-block-node/graph/badge.svg?token=OF6T6E8V7U)](https://codecov.io/github/hashgraph/hedera-block-node)
