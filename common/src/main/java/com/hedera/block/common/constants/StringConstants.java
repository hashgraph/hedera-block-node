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

package com.hedera.block.common.constants;

import static com.hedera.block.common.constants.ErrorMessageConstants.CREATING_INSTANCES_NOT_SUPPORTED;

/** A class that hold common String literals used across projects. */
public final class StringConstants {
    // FILES
    public static final String APPLICATION_PROPERTIES = "app.properties";
    public static final String LOGGING_PROPERTIES = "logging.properties";

    // FILE EXTENSIONS
    public static final String BLOCK_FILE_EXTENSION = ".blk";
    public static final String GZ_FILE_EXTENSION = ".gz";

    // PROJECT RELATED
    public static final String SERVICE_NAME = "BlockStreamService";
    public static final String CLIENT_STREAMING_METHOD_NAME = "publishBlockStream";
    public static final String SERVER_STREAMING_METHOD_NAME = "subscribeBlockStream";
    public static final String SINGLE_BLOCK_METHOD_NAME = "singleBlock";

    private StringConstants() {
        throw new UnsupportedOperationException(CREATING_INSTANCES_NOT_SUPPORTED);
    }
}
