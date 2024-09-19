/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hedera.block.server.events;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.InstantSource;

/**
 * Use this class to calculate and refresh the current liveness of a component based on a timeout
 * threshold.
 */
public final class LivenessCalculator {
    private final long timeoutThresholdMillis;
    private final InstantSource clock;
    private long lastMillis;

    /**
     * Once constructed, the liveness calculator will use the given clock and timeout threshold to
     * determine expiry.
     *
     * @param clock the clock to use for time calculations
     * @param timeoutThresholdMillis the timeout threshold in milliseconds
     */
    public LivenessCalculator(
            @NonNull final InstantSource clock, final long timeoutThresholdMillis) {

        this.clock = clock;
        this.timeoutThresholdMillis = timeoutThresholdMillis;
        this.lastMillis = clock.millis();
    }

    /**
     * Returns true if the timeout has expired based on the configuration window and the current
     * time.
     *
     * @return true if the timeout has expired
     */
    public boolean isTimeoutExpired() {
        return clock.millis() - lastMillis > timeoutThresholdMillis;
    }

    /**
     * Use refresh to reset the liveness calculator to the beginning of the configured threshold
     * window.
     */
    public void refresh() {
        lastMillis = clock.millis();
    }
}
