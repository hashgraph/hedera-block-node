package com.hedera.block.server.util;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.config.BlockNodeContextFactory;
import com.hedera.block.server.config.TestConfigBuilder;
import com.hedera.block.server.consumer.ConsumerConfig;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.extensions.sources.ClasspathFileConfigSource;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TestConfigUtil {
    private TestConfigUtil() {}

    @NonNull
    public static BlockNodeContext getSpyBlockNodeContext(Map<String, String> customProperties) throws IOException {
        // If customProperties is null, assign it an empty map
        if (customProperties == null) {
            customProperties = Collections.emptyMap();
        }

        // we still use the BlockNodeContextFactory to create the BlockNodeContext temporally,
        // but we will replace the configuration with a test configuration
        // sooner we will need to create a metrics mock, and never use the BlockNodeContextFactory.
        BlockNodeContext blockNodeContext = BlockNodeContextFactory.create();

        // create test configuration
        TestConfigBuilder testConfigBuilder = new TestConfigBuilder(true)
                .withSource(new ClasspathFileConfigSource(Path.of("app.properties")));

        for (Map.Entry<String, String> entry : customProperties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            testConfigBuilder = testConfigBuilder.withValue(key, value);
        }

        testConfigBuilder = testConfigBuilder.withConfigDataType(ConsumerConfig.class);

        Configuration testConfiguration = testConfigBuilder.getOrCreateConfig();

        BlockNodeContext spyBlockNodeContext = spy(blockNodeContext);
        when(spyBlockNodeContext.configuration()).thenReturn(testConfiguration);
        return spyBlockNodeContext;
    }

    public static BlockNodeContext getSpyBlockNodeContext() throws IOException {
        return getSpyBlockNodeContext(null);
    }
}
