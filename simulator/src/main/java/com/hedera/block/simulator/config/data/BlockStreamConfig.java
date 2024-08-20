package com.hedera.block.simulator.config.data;

import com.hedera.block.simulator.config.types.GenerationMode;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;

//TODO add block dir option to generate from directory
@ConfigData("blockStream")
public record BlockStreamConfig(@ConfigProperty(defaultValue = "SELF") GenerationMode generationMode) {

}

