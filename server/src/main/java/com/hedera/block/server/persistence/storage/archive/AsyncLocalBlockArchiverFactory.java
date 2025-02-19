// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.archive;

/**
 * TODO: add documentation
 */
public interface AsyncLocalBlockArchiverFactory {
    AsyncLocalBlockArchiver create(final long blockNumber);
}
