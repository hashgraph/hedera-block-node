package com.hedera.block.simulator.config.types;

/** The SimulatorMode enum defines the work modes of the block stream simulator. */
public enum SimulatorMode {
    /**
     * Indicates a work mode in which the simulator is working as both consumer and publisher.
     */
    BOTH,
    /**
     * Indicates a work mode in which the simulator is working in consumer mode.
     */
    CONSUMER,
    /**
     * Indicates a work mode in which the simulator is working in publisher mode.
     */
    PUBLISHER
}
