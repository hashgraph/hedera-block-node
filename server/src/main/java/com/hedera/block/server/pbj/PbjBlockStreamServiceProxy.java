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

import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SubscribeStreamRequest;
import com.hedera.hapi.block.SubscribeStreamResponse;
import java.util.concurrent.Flow;

public class PbjBlockStreamServiceProxy implements PbjBlockStreamService {

    private PbjBlockStreamService pbjBlockStreamService;

    public SingleBlockResponse singleBlock(SingleBlockRequest singleBlockRequest) {
        return pbjBlockStreamService.singleBlock(singleBlockRequest);
    }

    @Override
    public Flow.Subscriber<? super PublishStreamRequest> publishBlockStream(
            Flow.Subscriber<? super PublishStreamResponse> publishStreamRequest) {
        return pbjBlockStreamService.publishBlockStream(publishStreamRequest);
    }

    @Override
    public void subscribeBlockStream(
            SubscribeStreamRequest subscribeStreamRequest,
            Flow.Subscriber<? super SubscribeStreamResponse> responses) {
        pbjBlockStreamService.subscribeBlockStream(subscribeStreamRequest, responses);
    }
}
