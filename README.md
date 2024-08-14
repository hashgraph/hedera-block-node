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

# Usage

## Configuration

| Environment Variable            | Description                                                                                                   | Default Value |
|---------------------------------|---------------------------------------------------------------------------------------------------------------|---------------|
| persistence.storage.rootPath    | The root path for the storage, if not provided will attempt to create a `data` on the working dir of the app. | ./data        |
| consumer.timeoutThresholdMillis | Time to wait for subscribers before disconnecting in milliseconds                                             | 1500          |



# Staring locally:
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

# Running Tests
1) ./gradlew build


### Code Coverage
[![codecov](https://codecov.io/github/hashgraph/hedera-block-node/graph/badge.svg?token=OF6T6E8V7U)](https://codecov.io/github/hashgraph/hedera-block-node)
