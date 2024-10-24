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

import com.hedera.block.common.utils.ChunkUtils;
import com.hedera.block.simulator.Translator;
import com.hedera.block.simulator.config.data.BlockStreamConfig;
import com.hedera.block.simulator.config.data.GrpcConfig;
import com.hedera.hapi.block.protoc.BlockStreamServiceGrpc;
import com.hedera.hapi.block.protoc.PublishStreamRequest;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * The PublishStreamGrpcClientImpl class provides the methods to stream the block and block item.
 */
public class PublishStreamGrpcClientImpl implements PublishStreamGrpcClient {

    private final BlockStreamServiceGrpc.BlockStreamServiceStub stub;
    private final StreamObserver<PublishStreamRequest> requestStreamObserver;
    private final BlockStreamConfig blockStreamConfig;

    /**
     * Creates a new PublishStreamGrpcClientImpl instance.
     *
     * @param grpcConfig the gRPC configuration
     * @param blockStreamConfig the block stream configuration
     */
    @Inject
    public PublishStreamGrpcClientImpl(
            @NonNull GrpcConfig grpcConfig, @NonNull BlockStreamConfig blockStreamConfig) {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(grpcConfig.serverAddress(), grpcConfig.port())
                        .usePlaintext()
                        .build();
        stub = BlockStreamServiceGrpc.newStub(channel);
        PublishStreamObserver publishStreamObserver = new PublishStreamObserver();
        requestStreamObserver = stub.publishBlockStream(publishStreamObserver);
        this.blockStreamConfig = blockStreamConfig;
    }

    /**
     * The PublishStreamObserver class implements the StreamObserver interface to observe the
     * stream.
     */
    @Override
    public boolean streamBlockItem(List<BlockItem> blockItems) {

        List<com.hedera.hapi.block.stream.protoc.BlockItem> blockItemsProtoc = new ArrayList<>();
        for (BlockItem blockItem : blockItems) {
            blockItemsProtoc.add(Translator.fromPbj(blockItem));
        }

        requestStreamObserver.onNext(
                PublishStreamRequest.newBuilder().addAllBlockItems(blockItemsProtoc).build());

        return true;
    }

    /**
     * The PublishStreamObserver class implements the StreamObserver interface to observe the
     * stream.
     */
    @Override
    public boolean streamBlock(Block block) {
        List<com.hedera.hapi.block.stream.protoc.BlockItem> blockItemsProtoc = new ArrayList<>();
        for (BlockItem blockItem : block.items()) {
            blockItemsProtoc.add(Translator.fromPbj(blockItem));
        }

        List<List<com.hedera.hapi.block.stream.protoc.BlockItem>> streamingBatches =
                ChunkUtils.chunkify(blockItemsProtoc, blockStreamConfig.blockItemsBatchSize());
        for (List<com.hedera.hapi.block.stream.protoc.BlockItem> streamingBatch :
                streamingBatches) {
            requestStreamObserver.onNext(
                    PublishStreamRequest.newBuilder().addAllBlockItems(streamingBatch).build());
        }

        return true;
    }
}
