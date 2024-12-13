// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.write;

/**
 * A marker interface that groups all writers that operate on a local file
 * system.
 *
 * @param <V> the type of the value to be written
 */
interface LocalBlockWriter<V> extends BlockWriter<V> {}
