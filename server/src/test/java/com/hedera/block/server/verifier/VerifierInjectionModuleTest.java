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

package com.hedera.block.server.verifier;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.SubscriptionHandler;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.write.BlockWriter;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.util.TestConfigUtil;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.BlockItem;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VerifierInjectionModuleTest {

    @Mock private SubscriptionHandler<SubscribeStreamResponse> subscriptionHandler;
    @Mock private Notifier notifier;
    @Mock private BlockWriter<BlockItem> blockWriter;

    @Mock private ServiceStatus serviceStatus;

    @Test
    void testProvidesStreamValidatorBuilder() throws IOException {

        BlockNodeContext blockNodeContext = TestConfigUtil.getTestBlockNodeContext();

        // Call the method under test
        BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>> streamVerifier =
                new StreamVerifierImpl(
                        subscriptionHandler,
                        notifier,
                        blockWriter,
                        blockNodeContext,
                        serviceStatus);

        assertNotNull(streamVerifier);
    }
}
