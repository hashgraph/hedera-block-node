// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.verification.service;

import static java.lang.System.Logger.Level.INFO;

import com.hedera.hapi.block.BlockItemUnparsed;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;

/** No-op implementation of the {@link BlockVerificationService}. */
public class NoOpBlockVerificationService implements BlockVerificationService {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    /**
     * Constructs a no-op block verification service.
     */
    public NoOpBlockVerificationService() {
        // no-op
    }

    /**
     * Does nothing
     */
    @Override
    public void onBlockItemsReceived(@NonNull List<BlockItemUnparsed> blockItems) {
        LOGGER.log(INFO, "Using " + getClass().getName());
    }
}
