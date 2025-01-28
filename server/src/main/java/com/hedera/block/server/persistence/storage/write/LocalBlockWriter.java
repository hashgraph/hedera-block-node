// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.write;

/**
 * A marker interface that groups all writers that operate on a local file
 * system.
 *
 * @param <V> the type of the value to be written
 * @param <R> the type of the return value
 */
interface LocalBlockWriter<V, R> extends BlockWriter<V, R> {}
