// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.mode;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.block.simulator.grpc.PublishStreamGrpcServer;
import com.hedera.block.simulator.mode.impl.PublisherServerModeHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class PublisherServerModeHandlerTest {

    @Mock
    private PublishStreamGrpcServer publishStreamGrpcServer;

    private PublisherServerModeHandler publisherServerModeHandler;

    @Test
    void testStartThrowsUnsupportedOperationException() {
        publisherServerModeHandler = new PublisherServerModeHandler(publishStreamGrpcServer);

        assertThrows(UnsupportedOperationException.class, () -> publisherServerModeHandler.start());
    }
}
