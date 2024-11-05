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

import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.hapi.block.protoc.PublishStreamResponse;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.lang.System.Logger;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The PublishStreamObserver class provides the methods to observe the stream of
 * the published
 * stream.
 */
public class PublishStreamObserver implements StreamObserver<PublishStreamResponse> {

    private final Logger logger = System.getLogger(getClass().getName());
    private final AtomicBoolean streamEnabled;
    private final List<String> lastKnownStatuses;

    /**
     * Creates a new PublishStreamObserver instance.
     *
     * @param streamEnabled     is responsible for signaling, whether streaming
     *                          should continue
     * @param lastKnownStatuses the last known statuses
     */
    public PublishStreamObserver(
            @NonNull final AtomicBoolean streamEnabled, @NonNull final List<String> lastKnownStatuses) {
        this.streamEnabled = requireNonNull(streamEnabled);
        this.lastKnownStatuses = requireNonNull(lastKnownStatuses);
    }

    /** what will the stream observer do with the response from the server */
    @Override
    public void onNext(PublishStreamResponse publishStreamResponse) {
        lastKnownStatuses.add(publishStreamResponse.toString());
        logger.log(INFO, "Received Response: " + publishStreamResponse.toString());
    }

    /**
     * Responsible for stream observer behaviour, in case of error. For now, we will
     * stop the stream for every error. In the future we'd want to have a retry
     * mechanism depending on the error.
     */
    @Override
    public void onError(@NonNull final Throwable streamError) {
        streamEnabled.set(false);
        Status status = Status.fromThrowable(streamError);
        lastKnownStatuses.add(status.toString());
        logger.log(Logger.Level.ERROR, "Error %s with status %s.".formatted(streamError, status), streamError);
    }

    /** what will the stream observer do when the stream is completed */
    @Override
    public void onCompleted() {
        logger.log(INFO, "Completed");
    }
}
