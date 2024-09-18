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

import static org.junit.jupiter.api.Assertions.*;

import com.hedera.hapi.block.protoc.PublishStreamResponse;
import org.junit.jupiter.api.Test;

class PublishStreamObserverTest {

    @Test
    void onNext() {
        PublishStreamResponse response = PublishStreamResponse.newBuilder().build();
        PublishStreamObserver publishStreamObserver = new PublishStreamObserver();
        publishStreamObserver.onNext(response);
    }

    @Test
    void onError() {
        PublishStreamObserver publishStreamObserver = new PublishStreamObserver();
        publishStreamObserver.onError(new Throwable());
    }

    @Test
    void onCompleted() {
        PublishStreamObserver publishStreamObserver = new PublishStreamObserver();
        publishStreamObserver.onCompleted();
    }
}
