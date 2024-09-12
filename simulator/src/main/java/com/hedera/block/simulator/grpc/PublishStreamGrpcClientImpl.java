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
import javax.inject.Inject;

public class PublishStreamGrpcClientImpl implements PublishStreamGrpcClient {

    private final BlockStreamServiceGrpc.BlockStreamServiceStub stub;
    private final StreamObserver<PublishStreamRequest> requestStreamObserver;

    @Inject
    public PublishStreamGrpcClientImpl(@NonNull GrpcConfig grpcConfig) {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(grpcConfig.serverAddress(), grpcConfig.port())
                        .usePlaintext()
                        .build();
        stub = BlockStreamServiceGrpc.newStub(channel);
        PublishStreamObserver publishStreamObserver = new PublishStreamObserver();
        requestStreamObserver = stub.publishBlockStream(publishStreamObserver);
    }

    @Override
    public void streamBlockItem(BlockItem blockItem) {
        requestStreamObserver.onNext(
                PublishStreamRequest.newBuilder()
                        .setBlockItem(Translator.fromPbj(blockItem))
                        .build());
    }

    @Override
    public boolean streamBlock(Block block) {
        for (BlockItem blockItem : block.items()) {
            streamBlockItem(blockItem);
        }

        // wait for ack on the block
        // if and when the ack is received return true

        return true;
    }
}
