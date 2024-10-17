package com.hedera.block.simulator.mode;

public interface BlockStreamSimulatorHandler {
    /**
     * Initialize the needed dependencies, like grpc and block stream manager.
     */
    void init();

    /**
     * Starts the simulator and initiate streaming, depending on the working mode.
     */
    void start();
}
