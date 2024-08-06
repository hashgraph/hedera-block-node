package com.hedera.block.server.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockNodeContextFactoryTest {

        @Test
        void create_returnsBlockNodeContext() {
            BlockNodeContext context = BlockNodeContextFactory.create();

            assertNotNull(context.metrics());
            assertNotNull(context.configuration());
        }

}
