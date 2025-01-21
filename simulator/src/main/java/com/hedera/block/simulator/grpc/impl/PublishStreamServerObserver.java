// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.grpc.impl;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.hapi.block.protoc.PublishStreamResponse.Acknowledgement;
import com.hedera.hapi.block.protoc.PublishStreamResponse.BlockAcknowledgement;
import com.hedera.hapi.block.protoc.PublishStreamRequest;
import com.hedera.hapi.block.protoc.PublishStreamResponse;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.hedera.hapi.block.stream.protoc.BlockProof;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class PublishStreamServerObserver implements StreamObserver<PublishStreamRequest> {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // gRPC Components
    private final StreamObserver<PublishStreamResponse> responseObserver;

    public PublishStreamServerObserver(StreamObserver<PublishStreamResponse> responseObserver) {
        this.responseObserver = requireNonNull(responseObserver);
    }

    @Override
    public void onNext(PublishStreamRequest publishStreamRequest) {

        if (publishStreamRequest.hasBlockItems()) {
            final List<BlockItem> blockItemList = publishStreamRequest.getBlockItems().getBlockItemsList();
            if (blockItemList.getLast().hasBlockProof()) {
                final BlockProof blockProof = publishStreamRequest.getBlockItems().getBlockItemsList().getLast().getBlockProof();
                final PublishStreamResponse publishStreamResponse = handleBlockAckResponse(blockProof);

                responseObserver.onNext(publishStreamResponse);
            }
        }
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

    private PublishStreamResponse handleBlockAckResponse(BlockProof blockProof) {
        final long blockNumber = blockProof.getBlock();
        final BlockAcknowledgement blockAcknowledgement = BlockAcknowledgement.newBuilder().setBlockNumber(blockNumber).build();
        final Acknowledgement ack = Acknowledgement.newBuilder().setBlockAck(blockAcknowledgement).build();
        LOGGER.log(INFO, "Returning block acknowledgement for block number: %s".formatted(blockNumber));

        return PublishStreamResponse.newBuilder()
                .setAcknowledgement(ack)
                .build();
    }
}
