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

package com.hedera.block.server.mediator;


import com.hedera.block.protos.BlockStreamServiceGrpcProto;
import com.hedera.block.server.consumer.LiveStreamObserver;
import com.hedera.block.server.persistence.WriteThroughCacheHandler;
import com.hedera.block.server.persistence.cache.BlockCache;
import com.hedera.block.server.persistence.storage.BlockStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LiveStreamMediatorImplTest {

    @Mock
    private LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver1;

    @Mock
    private LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver2;

    @Mock
    private LiveStreamObserver<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> liveStreamObserver3;

    @Mock
    private BlockStorage<BlockStreamServiceGrpcProto.Block> blockStorage;

    @Mock
    private BlockCache<BlockStreamServiceGrpcProto.Block> blockCache;

    @Test
    public void testUnsubscribeAll() {

        final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamMediator =
                new LiveStreamMediatorImpl(new WriteThroughCacheHandler(blockStorage, blockCache));

        // Set up the subscribers
        streamMediator.subscribe(liveStreamObserver1);
        streamMediator.subscribe(liveStreamObserver2);
        streamMediator.subscribe(liveStreamObserver3);

        assertTrue(streamMediator.isSubscribed(liveStreamObserver1), "Expected the mediator to have liveStreamObserver1 subscribed");
        assertTrue(streamMediator.isSubscribed(liveStreamObserver2), "Expected the mediator to have liveStreamObserver2 subscribed");
        assertTrue(streamMediator.isSubscribed(liveStreamObserver3), "Expected the mediator to have liveStreamObserver3 subscribed");

        streamMediator.unsubscribeAll();

        assertFalse(streamMediator.isSubscribed(liveStreamObserver1), "Expected the mediator to have unsubscribed liveStreamObserver1");
        assertFalse(streamMediator.isSubscribed(liveStreamObserver2), "Expected the mediator to have unsubscribed liveStreamObserver2");
        assertFalse(streamMediator.isSubscribed(liveStreamObserver3), "Expected the mediator to have unsubscribed liveStreamObserver3");
    }

    @Test
    public void testUnsubscribeEach() {

        final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamMediator =
                new LiveStreamMediatorImpl(new WriteThroughCacheHandler(blockStorage, blockCache));

        // Set up the subscribers
        streamMediator.subscribe(liveStreamObserver1);
        streamMediator.subscribe(liveStreamObserver2);
        streamMediator.subscribe(liveStreamObserver3);

        assertTrue(streamMediator.isSubscribed(liveStreamObserver1), "Expected the mediator to have liveStreamObserver1 subscribed");
        assertTrue(streamMediator.isSubscribed(liveStreamObserver2), "Expected the mediator to have liveStreamObserver2 subscribed");
        assertTrue(streamMediator.isSubscribed(liveStreamObserver3), "Expected the mediator to have liveStreamObserver3 subscribed");

        streamMediator.unsubscribe(liveStreamObserver1);
        assertFalse(streamMediator.isSubscribed(liveStreamObserver1), "Expected the mediator to have unsubscribed liveStreamObserver1");

        streamMediator.unsubscribe(liveStreamObserver2);
        assertFalse(streamMediator.isSubscribed(liveStreamObserver2), "Expected the mediator to have unsubscribed liveStreamObserver2");

        streamMediator.unsubscribe(liveStreamObserver3);
        assertFalse(streamMediator.isSubscribed(liveStreamObserver3), "Expected the mediator to have unsubscribed liveStreamObserver3");
    }

    @Test
    public void testMediatorPersistenceWithoutSubscribers() {

        final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamMediator =
                new LiveStreamMediatorImpl(new WriteThroughCacheHandler(blockStorage, blockCache));

        final BlockStreamServiceGrpcProto.Block newBlock = BlockStreamServiceGrpcProto.Block.newBuilder().build();

        // Acting as a producer, notify the mediator of a new block
        streamMediator.notifyAll(newBlock);

        // Confirm the block was persisted to storage and cache
        // even though there are no subscribers
        verify(blockStorage).write(newBlock);
        verify(blockCache).insert(newBlock);
    }

    @Test
    public void testMediatorNotifyAll() {

        final StreamMediator<BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> streamMediator =
                new LiveStreamMediatorImpl(new WriteThroughCacheHandler(blockStorage, blockCache));

        // Set up the subscribers
        streamMediator.subscribe(liveStreamObserver1);
        streamMediator.subscribe(liveStreamObserver2);
        streamMediator.subscribe(liveStreamObserver3);

        assertTrue(streamMediator.isSubscribed(liveStreamObserver1), "Expected the mediator to have liveStreamObserver1 subscribed");
        assertTrue(streamMediator.isSubscribed(liveStreamObserver2), "Expected the mediator to have liveStreamObserver2 subscribed");
        assertTrue(streamMediator.isSubscribed(liveStreamObserver3), "Expected the mediator to have liveStreamObserver3 subscribed");

        final BlockStreamServiceGrpcProto.Block newBlock = BlockStreamServiceGrpcProto.Block.newBuilder().build();

        // Acting as a producer, notify the mediator of a new block
        streamMediator.notifyAll(newBlock);

        // Confirm each subscriber was notified of the new block
        verify(liveStreamObserver1).notify(newBlock);
        verify(liveStreamObserver2).notify(newBlock);
        verify(liveStreamObserver3).notify(newBlock);

        // Confirm the block was persisted to storage and cache
        verify(blockStorage).write(newBlock);
        verify(blockCache).insert(newBlock);
    }

}
