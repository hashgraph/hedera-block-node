package com.hedera.block.server;

import com.google.protobuf.Descriptors;
import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.persistence.BlockPersistenceHandler;
import com.hedera.block.server.persistence.WriteThroughCacheHandler;
import com.hedera.block.server.persistence.cache.BlockCache;
import com.hedera.block.server.persistence.cache.LRUCache;
import com.hedera.block.server.persistence.storage.BlockStorage;
import com.hedera.block.server.persistence.storage.FileSystemBlockStorage;
import io.grpc.stub.StreamObserver;
import io.helidon.config.Config;
import io.helidon.webserver.grpc.GrpcService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.hedera.block.server.Constants.BLOCKNODE_STORAGE_ROOT_PATH_KEY;

public class BlockStreamService implements GrpcService {

    private final Logger logger = Logger.getLogger("BlockStreamService");
    private final BlockPersistenceHandler blockPersistenceHandler;

    public BlockStreamService() {
        try {

            Config config = Config.create();
            Config.global(config);

            Path blockNodeRootPath = Path.of(Config
                    .global()
                    .get(BLOCKNODE_STORAGE_ROOT_PATH_KEY)
                    .asString()
                    .get());

            if (!blockNodeRootPath.isAbsolute()) {
                throw new IllegalArgumentException(BLOCKNODE_STORAGE_ROOT_PATH_KEY+ " must be an absolute path");
            }

            if (Files.notExists(blockNodeRootPath)) {
                Files.createDirectory(blockNodeRootPath);
                logger.info("Created block node root directory: " + blockNodeRootPath);
            } else {
                logger.info("Block node root directory exists: " + blockNodeRootPath);
            }

            BlockStorage blockStorage = new FileSystemBlockStorage(blockNodeRootPath);
            BlockCache blockCache = new LRUCache(10L);
            this.blockPersistenceHandler = new WriteThroughCacheHandler(blockStorage, blockCache);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Descriptors.FileDescriptor proto() {
        return BlockStreamServiceGrpcProto.getDescriptor();
    }

    @Override
    public String serviceName() {
        return "BlockStreamGrpc";
    }

    @Override
    public void update(Routing routing) {
        routing.clientStream("StreamSink", this::streamSink);
        routing.serverStream("StreamSource", this::streamSource);
    }

    public StreamObserver<BlockStreamServiceGrpcProto.Block> streamSink(StreamObserver<BlockStreamServiceGrpcProto.BlockResponse> observer) {
        logger.fine("Executing streamSink method");

        return new InboundBlockStreamObserver(observer, blockPersistenceHandler);
    }


    public void streamSource(BlockStreamServiceGrpcProto.BlockRequest blockRequest, StreamObserver<BlockStreamServiceGrpcProto.Block> observer) {
        logger.fine("Executing streamSource method");
        logger.info("Block request: " + blockRequest);

        StreamGenerator streamGenerator = new StreamGenerator(20, this.blockPersistenceHandler, observer);
        streamGenerator.generate(blockRequest.getId());
    }

    private static class StreamGenerator {

        private final int inactivityLimit;
        private final BlockPersistenceHandler blockPersistenceHandler;
        private final StreamObserver<BlockStreamServiceGrpcProto.Block> observer;

        StreamGenerator(int inactivityLimit,
                        BlockPersistenceHandler blockPersistenceHandler,
                        StreamObserver<BlockStreamServiceGrpcProto.Block> observer) {
            this.inactivityLimit = inactivityLimit;
            this.blockPersistenceHandler = blockPersistenceHandler;
            this.observer = observer;
        }

        public void generate(long blockId) {

            AtomicInteger inactivityCounter = new AtomicInteger();
            while (true) {
                Queue<BlockStreamServiceGrpcProto.Block> blocks = blockPersistenceHandler.readFrom(blockId, x -> true);
                blockId = generate(blockId, inactivityCounter, blocks);
                if (blockId == -1) {
                    break;
                }
            }
        }

        private long generate(long blockId, AtomicInteger inactivityCounter, Queue<BlockStreamServiceGrpcProto.Block> blocks) {

            if (blocks.isEmpty()) {
                if (inactivityCounter.get() >= inactivityLimit) {
//                logger.debug("Thread inactivity limit reached.  Disconnecting client.");
//                logger.debug("Object: " + this);
                    observer.onCompleted();
                    return -1;
                }

                try {
                    Thread.sleep(1000);
                    inactivityCounter.incrementAndGet();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                return blockId;
            }

//    logger.debug("Generator: Id: " + state);
//    logger.debug("Generator: block count: " + blocks.size());

            inactivityCounter.set(0);

            for (BlockStreamServiceGrpcProto.Block block : blocks) {
                observer.onNext(block);
                blockId = block.getId();
            }

            blockId++;

            return blockId;
        }
    }
}


