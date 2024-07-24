package com.hedera.block.server;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.mediator.StreamMediator;
import com.hedera.block.server.persistence.BlockPersistenceHandler;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockStreamServiceTest {

    private final long TIMEOUT_THRESHOLD_MILLIS = 52L;


    @Mock
    private StreamObserver<BlockStreamServiceGrpcProto.Block> responseObserver;

    @Mock
    private BlockPersistenceHandler<BlockStreamServiceGrpcProto.Block> blockPersistenceHandler;

    @Mock
    private StreamMediator<
                BlockStreamServiceGrpcProto.Block,
                BlockStreamServiceGrpcProto.BlockResponse>
            streamMediator;

    @Test
    void getBlockHappyPath() {
        BlockStreamServiceGrpcProto.Block block = BlockStreamServiceGrpcProto.Block.newBuilder().setId(1).build();
        BlockStreamService blockStreamService = new BlockStreamService(TIMEOUT_THRESHOLD_MILLIS, streamMediator, blockPersistenceHandler);
        when(blockPersistenceHandler.read(1)).thenReturn(Optional.of(BlockStreamServiceGrpcProto.Block.newBuilder().setId(1).build()));
        blockStreamService.getBlock(block, responseObserver);
        verify(responseObserver, times(1)).onNext(block);
    }

    @Test
    void getBlockErrorPath() {
        BlockStreamServiceGrpcProto.Block block = BlockStreamServiceGrpcProto.Block.newBuilder().setId(1).build();
        BlockStreamService blockStreamService = new BlockStreamService(TIMEOUT_THRESHOLD_MILLIS, streamMediator, blockPersistenceHandler);
        when(blockPersistenceHandler.read(1)).thenReturn(Optional.empty());
        blockStreamService.getBlock(block, responseObserver);
        verify(responseObserver, times(1)).onNext(BlockStreamServiceGrpcProto.Block.newBuilder().setId(0).build());
    }
}