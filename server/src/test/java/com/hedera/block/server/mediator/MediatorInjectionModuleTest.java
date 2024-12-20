// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.mediator;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediatorInjectionModuleTest {

    @Mock
    private ServiceStatus serviceStatus;

    @BeforeEach
    void setup() {
        // Any setup before each test can be done here
    }

    @Test
    void testProvidesStreamMediator() throws IOException {

        BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();

        // Call the method under test
        StreamMediator<List<BlockItemUnparsed>, SubscribeStreamResponseUnparsed> streamMediator =
                MediatorInjectionModule.providesLiveStreamMediator(blockNodeContext, serviceStatus);

        // Verify that the streamMediator is correctly instantiated
        assertNotNull(streamMediator);
        assertInstanceOf(LiveStreamMediatorImpl.class, streamMediator);
    }

    @Test
    void testNoOpProvidesStreamMediator() throws IOException {

        Map<String, String> properties = Map.of("mediator.type", "NOOP");
        BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext(properties);

        // Call the method under test
        StreamMediator<List<BlockItemUnparsed>, SubscribeStreamResponseUnparsed> streamMediator =
                MediatorInjectionModule.providesLiveStreamMediator(blockNodeContext, serviceStatus);

        // Verify that the streamMediator is correctly instantiated
        assertNotNull(streamMediator);
        assertInstanceOf(NoOpLiveStreamMediator.class, streamMediator);
    }
}
