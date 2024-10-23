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

import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.simulator.Translator;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.hapi.block.protoc.BlockStreamServiceGrpc;
import com.hedera.hapi.block.protoc.PublishStreamRequest;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

/**
 * The PublishStreamGrpcClientImpl class provides the methods to stream the block and block item.
 */
public class PublishStreamGrpcClientImpl implements PublishStreamGrpcClient {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final BlockStreamServiceGrpc.BlockStreamServiceStub stub;
    private final StreamObserver<PublishStreamRequest> requestStreamObserver;
    private final AtomicBoolean allowNext = new AtomicBoolean(true);

    /**
     * Creates a new PublishStreamGrpcClientImpl instance.
     *
     * @param grpcConfig the gRPC configuration
     */
    @Inject
    public PublishStreamGrpcClientImpl(@NonNull GrpcConfig grpcConfig) {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(grpcConfig.serverAddress(), grpcConfig.port())
                        .usePlaintext()
                        .build();
        // SEVERE: Error: io.grpc.StatusRuntimeException: CANCELLED: Failed to stream message
        //        stub = BlockStreamServiceGrpc.newStub(channel).withMaxOutboundMessageSize(100000);
        //        stub =
        // BlockStreamServiceGrpc.newStub(channel).withMaxOutboundMessageSize(16777202);
        stub = BlockStreamServiceGrpc.newStub(channel);

        PublishStreamObserver publishStreamObserver = new PublishStreamObserver(allowNext);
        requestStreamObserver = stub.publishBlockStream(publishStreamObserver);
    }

    /**
     * The PublishStreamObserver class implements the StreamObserver interface to observe the
     * stream.
     */
    @Override
    public boolean streamBlockItem(BlockItem blockItem) {
        //        try {
        // worked for 30 mins but failed after that
        //            Duration nanosDuration = Duration.ofNanos(150000);
        //            Duration nanosDuration = Duration.ofNanos(1000);
        //            Thread.sleep(nanosDuration);
        //        } catch (InterruptedException e) {
        //            throw new RuntimeException(e);
        //        }

        if (allowNext.get()) {
            requestStreamObserver.onNext(
                    PublishStreamRequest.newBuilder()
                            .setBlockItem(Translator.fromPbj(blockItem))
                            .build());
        } else {
            LOGGER.log(ERROR, "Not allowed to send next block item");
        }

        return allowNext.get();
    }

    /**
     * The PublishStreamObserver class implements the StreamObserver interface to observe the
     * stream.
     */
    @Override
    public boolean streamBlock(Block block) {
        for (int count = 0; count < block.items().size(); count++) {
            if (!streamBlockItem(block.items().get(count))) {
                LOGGER.log(ERROR, "Count was: " + count);

                if (count == 0) {
                    LOGGER.log(ERROR, "First block item: " + block.items().get(count));
                } else {
                    LOGGER.log(ERROR, "Previous block item: " + block.items().get(count - 1));
                }
                return false;
            }
        }

        return true;
    }
}
