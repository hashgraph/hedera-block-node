package com.hedera.block.simulator.mode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.generator.BlockStreamManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CombinedModeHandlerTest {

    @Mock
    private BlockStreamConfig blockStreamConfig;

    private CombinedModeHandler combinedModeHandler;

    @Test
    public void testStartThrowsUnsupportedOperationException() {
        MockitoAnnotations.openMocks(this);
        combinedModeHandler = new CombinedModeHandler(blockStreamConfig);
        BlockStreamManager blockStreamManager = mock(BlockStreamManager.class);

        assertThrows(
                UnsupportedOperationException.class,
                () -> combinedModeHandler.start(blockStreamManager)
        );
    }
}
