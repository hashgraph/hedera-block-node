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

package com.hedera.block.server;

/** Constants used in the BlockNode service. */
public final class Constants {
    /** Constant mapped to the semantic name of the Block Node root directory */
    public static final String BLOCK_NODE_ROOT_DIRECTORY_SEMANTIC_NAME =
            "Block Node Root Directory";

    /** Constant mapped to the name of the BlockStream service in the .proto file */
    public static final String SERVICE_NAME_BLOCK_STREAM = "BlockStreamService";

    /** Constant mapped to the name of the BlockAccess service in the .proto file */
    public static final String SERVICE_NAME_BLOCK_ACCESS = "BlockAccessService";

    /** Constant mapped to the publishBlockStream service method name in the .proto file */
    public static final String CLIENT_STREAMING_METHOD_NAME = "publishBlockStream";

    /** Constant mapped to the subscribeBlockStream service method name in the .proto file */
    public static final String SERVER_STREAMING_METHOD_NAME = "subscribeBlockStream";

    /** Constant mapped to the singleBlock service method name in the .proto file */
    public static final String SINGLE_BLOCK_METHOD_NAME = "singleBlock";

    /** Constant defining the block file extension */
    public static final String BLOCK_FILE_EXTENSION = ".blk";

    /** Constant defining the compressed file extension */
    public static final String COMPRESSED_FILE_EXTENSION = ".zstd";

    private Constants() {}
}
