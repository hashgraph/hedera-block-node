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

package com.hedera.block.simulator;

/** The Constants class defines the constants for the block simulator. */
public final class Constants {
    /** The file extension for block files. */
    public static final String RECORD_EXTENSION = ".blk";

    /** postfix for gzip files */
    public static final String GZ_EXTENSION = ".gz";

    /**
     * Used for converting nanoseconds to milliseconds and vice versa
     */
    public static final int NANOS_PER_MILLI = 1_000_000;

    /** Constructor to prevent instantiation. this is only a utility class */
    private Constants() {}
}
