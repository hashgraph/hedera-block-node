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

import com.hedera.hapi.block.protoc.PublishStreamResponse;
import io.grpc.stub.StreamObserver;
import java.lang.System.Logger;

/**
 * The PublishStreamObserver class provides the methods to observe the stream of the published
 * stream.
 */
public class PublishStreamObserver implements StreamObserver<PublishStreamResponse> {

    private final Logger logger = System.getLogger(getClass().getName());

    /** Creates a new PublishStreamObserver instance. */
    public PublishStreamObserver() {}

    /** what will the stream observer do with the response from the server */
    @Override
    public void onNext(PublishStreamResponse publishStreamResponse) {
        logger.log(Logger.Level.INFO, "Received Response: " + publishStreamResponse.toString());
    }

    /** what will the stream observer do when an error occurs */
    @Override
    public void onError(Throwable throwable) {
        logger.log(Logger.Level.ERROR, "Error: " + throwable.toString());
        // @todo(286) - handle the error
    }

    /** what will the stream observer do when the stream is completed */
    @Override
    public void onCompleted() {
        logger.log(Logger.Level.DEBUG, "Completed");
    }
}
