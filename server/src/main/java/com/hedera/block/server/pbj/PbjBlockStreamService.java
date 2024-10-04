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

package com.hedera.block.server.pbj;

import static com.hedera.block.server.Constants.SERVICE_NAME;

import com.hedera.pbj.runtime.grpc.ServiceInterface;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Arrays;
import java.util.List;

public interface PbjBlockStreamService extends ServiceInterface {
    enum BlockStreamMethod implements Method {
        singleBlock,
        publishBlockStream,
        subscribeBlockStream
    }

    @NonNull
    default String serviceName() {
        return SERVICE_NAME;
    }

    @NonNull
    default String fullName() {
        return "com.hedera.hapi.block." + SERVICE_NAME;
    }

    @NonNull
    default List<Method> methods() {
        return Arrays.asList(BlockStreamMethod.values());
    }
}
