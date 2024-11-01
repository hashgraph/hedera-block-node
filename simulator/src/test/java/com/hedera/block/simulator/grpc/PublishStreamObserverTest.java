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

package com.hedera.block.simulator.grpc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.hapi.block.protoc.PublishStreamResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PublishStreamObserverTest {

    @Test
    void onNext() {
        PublishStreamResponse response = PublishStreamResponse.newBuilder().build();
        AtomicBoolean streamEnabled = new AtomicBoolean(true);

        PublishStreamObserver publishStreamObserver = new PublishStreamObserver(streamEnabled);
        publishStreamObserver.onNext(response);
        assertTrue(streamEnabled.get(), "allowNext should remain true after onCompleted");
    }

    @Test
    void onError() {
        AtomicBoolean streamEnabled = new AtomicBoolean(true);
        PublishStreamObserver publishStreamObserver = new PublishStreamObserver(streamEnabled);

        publishStreamObserver.onError(new Throwable());
        assertFalse(streamEnabled.get(), "allowNext should be set to false after onError");
    }

    @Test
    void onCompleted() {
        AtomicBoolean streamEnabled = new AtomicBoolean(true);
        PublishStreamObserver publishStreamObserver = new PublishStreamObserver(streamEnabled);

        publishStreamObserver.onCompleted();
        assertTrue(streamEnabled.get(), "allowNext should remain true after onCompleted");
    }
}
