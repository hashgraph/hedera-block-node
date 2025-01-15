// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.grpc.impl;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.hapi.block.protoc.PublishStreamRequest;
import com.hedera.hapi.block.protoc.PublishStreamResponse;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class PublishStreamServerObserver implements StreamObserver<PublishStreamRequest> {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // gRPC Components
    private final StreamObserver<PublishStreamResponse> responseObserver;

    public PublishStreamServerObserver(StreamObserver<PublishStreamResponse> responseObserver) {
        this.responseObserver = requireNonNull(responseObserver);
    }

    @Override
    public void onNext(PublishStreamRequest publishStreamRequest) {
        LOGGER.log(INFO, publishStreamRequest.getBlockItems());
        // send block ack. if there is a block proof in the set
        // send item ack. if there is no block proof
    }

    @Override
    public void onError(@NonNull final Throwable streamError) {
        Status status = Status.fromThrowable(streamError);
        LOGGER.log(ERROR, "Error %s with status %s.".formatted(streamError, status), streamError);
    }

    @Override
    public void onCompleted() {
        responseObserver.onCompleted();
        LOGGER.log(INFO, "Completed");
    }
}
