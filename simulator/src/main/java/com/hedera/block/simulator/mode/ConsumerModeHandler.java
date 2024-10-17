package com.hedera.block.simulator.mode;

public class ConsumerModeHandler implements BlockStreamSimulatorHandler {
    /**
     * Initialize the needed dependencies, like grpc and block stream manager.
     */
    @Override
    public void init() {
        throw new UnsupportedOperationException();
    }

    /**
     * Starts the simulator and initiate streaming, depending on the working mode.
     */
    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }
}
