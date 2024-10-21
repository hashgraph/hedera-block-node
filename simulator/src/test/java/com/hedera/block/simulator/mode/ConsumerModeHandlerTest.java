package com.hedera.block.simulator.mode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.generator.BlockStreamManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConsumerModeHandlerTest {

    @Mock
    private BlockStreamConfig blockStreamConfig;

    private ConsumerModeHandler consumerModeHandler;

    @Test
    public void testStartThrowsUnsupportedOperationException() {
        MockitoAnnotations.openMocks(this);
        consumerModeHandler = new ConsumerModeHandler(blockStreamConfig);
        BlockStreamManager blockStreamManager = mock(BlockStreamManager.class);

        assertThrows(
                UnsupportedOperationException.class,
                () -> consumerModeHandler.start(blockStreamManager)
        );
    }
}
