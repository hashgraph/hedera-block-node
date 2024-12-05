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

package com.hedera.block.simulator.grpc.impl;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.hapi.block.protoc.PublishStreamResponse;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of StreamObserver that handles responses from the block publishing stream.
 * This class processes server responses and manages the stream state based on server feedback.
 */
public class PublishStreamObserver implements StreamObserver<PublishStreamResponse> {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // State
    private final AtomicBoolean streamEnabled;
    private final List<String> lastKnownStatuses;

    /**
     * Creates a new PublishStreamObserver instance.
     *
     * @param streamEnabled Controls whether streaming should continue
     * @param lastKnownStatuses List to store the most recent status messages
     * @throws NullPointerException if any parameter is null
     */
    public PublishStreamObserver(
            @NonNull final AtomicBoolean streamEnabled, @NonNull final List<String> lastKnownStatuses) {
        this.streamEnabled = requireNonNull(streamEnabled);
        this.lastKnownStatuses = requireNonNull(lastKnownStatuses);
    }

    /**
     * Processes responses from the server, storing status information.
     *
     * @param publishStreamResponse The response received from the server
     */
    @Override
    public void onNext(PublishStreamResponse publishStreamResponse) {
        lastKnownStatuses.add(publishStreamResponse.toString());
        LOGGER.log(INFO, "Received Response: " + publishStreamResponse.toString());
    }

    /**
     * Handles stream errors by disabling the stream and logging the error.
     * Currently stops the stream for all errors, but could be enhanced with
     * retry logic in the future.
     *
     * @param streamError The error that occurred during streaming
     */
    @Override
    public void onError(@NonNull final Throwable streamError) {
        streamEnabled.set(false);
        Status status = Status.fromThrowable(streamError);
        lastKnownStatuses.add(status.toString());
        LOGGER.log(ERROR, "Error %s with status %s.".formatted(streamError, status), streamError);
    }

    /**
     * Handles stream completion by logging the event.
     */
    @Override
    public void onCompleted() {
        LOGGER.log(INFO, "Completed");
    }
}