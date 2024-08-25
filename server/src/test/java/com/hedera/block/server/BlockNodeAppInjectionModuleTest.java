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

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.util.TestConfigUtil;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BlockNodeAppInjectionModuleTest {
    @Test
    void testProvideBlockNodeContext() throws IOException {
        BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();

        Assertions.assertNotNull(blockNodeContext);
        Assertions.assertNotNull(blockNodeContext.configuration());
        Assertions.assertNotNull(blockNodeContext.metricsService());
    }
}
