package com.hedera.block.simulator.config.data;

import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;
import com.swirlds.config.api.validation.annotation.Max;
import com.swirlds.config.api.validation.annotation.Min;

@ConfigData("grpc")
public record GrpcConfig(
        @ConfigProperty(defaultValue = "localhost") @Min(0) @Max(65535) String serverAddress,
        @ConfigProperty(defaultValue = "8080") @Min(0) @Max(65535) int port
) {
}
