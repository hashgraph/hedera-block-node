# Server Configuration

The components of the Hedera Block Node all support loading configuration via the
environment.

## Default Values

The default configuration allows users to quickly get up and running without having to configure anything. This provides
ease of use at the trade-off of some insecure default configuration. Most configuration settings have appropriate
defaults and can be left unchanged. It is recommended to browse the properties below and adjust to your needs.

| Environment Variable | Description | Default Value |
|:---|:---|---:|
| PERSISTENCE_STORAGE_LIVE_ROOT_PATH | The root path for the live storage. The provided path must be absolute! | |
| PERSISTENCE_STORAGE_ARCHIVE_ROOT_PATH | The root path for the archive storage. The provided path must be absolute! | |
| PERSISTENCE_STORAGE_TYPE | Type of the persistence storage | BLOCK_AS_LOCAL_FILE |
| CONSUMER_TIMEOUT_THRESHOLD_MILLIS | Time to wait for subscribers before disconnecting in milliseconds | 1500 |
| SERVICE_DELAY_MILLIS | Service shutdown delay in milliseconds | 500 |
| MEDIATOR_RING_BUFFER_SIZE | Size of the ring buffer used by the mediator (must be a power of 2) | 67108864 |
| NOTIFIER_RING_BUFFER_SIZE | Size of the ring buffer used by the notifier (must be a power of 2) | 2048 |
| SERVER_PORT                       | The port the server will listen on                                                                            | 8080          |
| SERVER_MAX_MESSAGE_SIZE_BYTES     | The maximum size of a message frame in bytes                                                                  | 1048576       |
