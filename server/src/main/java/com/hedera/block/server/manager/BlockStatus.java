// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.manager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple bitmask-based status object for each block.
 * We only track whether it is "persisted" or "verified".
 */
public class BlockStatus {
    private static final int PERSISTED = 1 << 0; // 1
    private static final int VERIFIED = 1 << 1; // 2

    private final AtomicInteger state = new AtomicInteger(0);

    public void setPersisted() {
        setBits(PERSISTED);
    }

    public void setVerified() {
        setBits(VERIFIED);
    }

    public boolean isPersisted() {
        return (state.get() & PERSISTED) != 0;
    }

    public boolean isVerified() {
        return (state.get() & VERIFIED) != 0;
    }

    /**
     * Atomically sets the given bits in 'state'.
     * Returns true if this call changed at least one bit from 0 to 1.
     */
    private boolean setBits(int bitsToSet) {
        int oldVal, newVal;
        do {
            oldVal = state.get();
            newVal = oldVal | bitsToSet;
            if (oldVal == newVal) {
                // The bits are already set; no change
                return false;
            }
        } while (!state.compareAndSet(oldVal, newVal));
        return true;
    }
}
