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

package com.hedera.block.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.persistence.BlockPersistenceHandler;
import io.grpc.stub.StreamObserver;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockStreamServiceTest {

    private final long TIMEOUT_THRESHOLD_MILLIS = 52L;

    @Mock private StreamObserver<BlockStreamServiceGrpcProto.Block> responseObserver;

    @Mock
    private BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler;

    @Mock
    private StreamMediator<
                    BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse>
            streamMediator;

    @Test
    void getBlockHappyPath() {
        BlockStreamServiceGrpcProto.Block block =
                BlockStreamServiceGrpcProto.Block.newBuilder().setId(1).build();
        BlockStreamService blockStreamService =
                new BlockStreamService(
                        TIMEOUT_THRESHOLD_MILLIS, streamMediator, blockPersistenceHandler);
        when(blockPersistenceHandler.read(1))
                .thenReturn(
                        Optional.of(
                                BlockStreamServiceGrpcProto.Block.newBuilder().setId(1).build()));
        blockStreamService.getBlock(block, responseObserver);
        verify(responseObserver, times(1)).onNext(block);
    }

    @Test
    void getBlockErrorPath() {
        BlockStreamServiceGrpcProto.Block block =
                BlockStreamServiceGrpcProto.Block.newBuilder().setId(1).build();
        BlockStreamService blockStreamService =
                new BlockStreamService(
                        TIMEOUT_THRESHOLD_MILLIS, streamMediator, blockPersistenceHandler);
        when(blockPersistenceHandler.read(1)).thenReturn(Optional.empty());
        blockStreamService.getBlock(block, responseObserver);
        verify(responseObserver, times(1))
                .onNext(BlockStreamServiceGrpcProto.Block.newBuilder().setId(0).build());
    }
}
