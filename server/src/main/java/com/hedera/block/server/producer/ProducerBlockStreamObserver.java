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

package com.hedera.block.server.producer;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.mediator.StreamMediator;
import io.grpc.stub.StreamObserver;

/**
 * The ProducerBlockStreamObserver class plugs into Helidon's server-initiated bidirectional
 * gRPC service implementation.  Helidon calls methods on this class as networking events occur
 * with the connection to the upstream producer (e.g. blocks streamed from the Consensus Node to
 * the server).
 */
public class ProducerBlockStreamObserver implements StreamObserver<BlockStreamServiceGrpcProto.Block> {

    private final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamMediator;
    private final StreamObserver<BlockStreamServiceGrpcProto.BlockResponse> responseStreamObserver;
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    /**
     * Constructor for the ProducerBlockStreamObserver class.  It is responsible for calling the mediator with blocks
     * as they arrive from the upstream producer.  It also sends responses back to the upstream producer via the
     * responseStreamObserver.
     *
     * @param streamMediator the stream mediator
     * @param responseStreamObserver the response stream observer
     */
    public ProducerBlockStreamObserver(final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamMediator,
                                       final StreamObserver<BlockStreamServiceGrpcProto.BlockResponse> responseStreamObserver) {
        this.streamMediator = streamMediator;
        this.responseStreamObserver = responseStreamObserver;
    }

    /**
     * Helidon triggers this method when it receives a new block from the upstream producer.  The method notifies all
     * the mediator subscribers and sends a response back to the upstream producer.
     *
     * @param block the block streamed from the upstream producer
     */
    @Override
    public void onNext(final BlockStreamServiceGrpcProto.Block block) {
        streamMediator.notifyAll(block);

        final BlockStreamServiceGrpcProto.BlockResponse blockResponse = BlockStreamServiceGrpcProto.BlockResponse.newBuilder().setId(block.getId()).build();
        responseStreamObserver.onNext(blockResponse);
    }

    /**
     * Helidon triggers this method when an error occurs on the bidirectional stream to the upstream producer.
     *
     * @param t the error occurred on the stream
     */
    @Override
    public void onError(final Throwable t) {
        LOGGER.log(System.Logger.Level.ERROR, "onError method called with the exception: " + t.getMessage());
    }

    /**
     * Helidon triggers this method when the bidirectional stream to the upstream producer is completed.
     * Unsubscribe all the observers from the mediator.
     */
    @Override
    public void onCompleted() {
        LOGGER.log(System.Logger.Level.DEBUG, "ProducerBlockStreamObserver completed");
        streamMediator.unsubscribeAll();
        LOGGER.log(System.Logger.Level.DEBUG, "Unsubscribed all downstream consumers");
    }
}
