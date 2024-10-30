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
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.lang.System.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The PublishStreamObserver class provides the methods to observe the stream of the published
 * stream.
 */
public class PublishStreamObserver implements StreamObserver<PublishStreamResponse> {

    private final Logger logger = System.getLogger(getClass().getName());
    private final AtomicBoolean allowNext;

    /** Creates a new PublishStreamObserver instance. */
    public PublishStreamObserver(final AtomicBoolean allowNext) {
        this.allowNext = allowNext;
    }

    /** what will the stream observer do with the response from the server */
    @Override
    public void onNext(PublishStreamResponse publishStreamResponse) {
        logger.log(Logger.Level.INFO, "Received Response: " + publishStreamResponse.toString());
    }

    /** Responsible for stream observer behaviour, in case of error. For now, we will stop the stream for every error. In the future we'd want to have a retry mechanism depending on the error. */
    @Override
    public void onError(Throwable throwable) {
        Status status = Status.fromThrowable(throwable);
        logger.log(Logger.Level.ERROR, "Error: " + throwable.toString() + " with status code: " + status.getCode());
        allowNext.set(false);
    }

    /** what will the stream observer do when the stream is completed */
    @Override
    public void onCompleted() {
        logger.log(Logger.Level.DEBUG, "Completed");
    }
}
