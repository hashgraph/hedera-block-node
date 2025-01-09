// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.grpc.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.hapi.block.protoc.PublishStreamResponse;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PublishStreamObserverTest {

    @Test
    void onNext() {
        PublishStreamResponse response = PublishStreamResponse.newBuilder().build();
        AtomicBoolean streamEnabled = new AtomicBoolean(true);
        ArrayDeque<String> lastKnownStatuses = new ArrayDeque<>();
        final int lastKnownStatusesCapacity = 10;
        PublishStreamObserver publishStreamObserver =
                new PublishStreamObserver(streamEnabled, lastKnownStatuses, lastKnownStatusesCapacity);

        publishStreamObserver.onNext(response);
        assertTrue(streamEnabled.get(), "streamEnabled should remain true after onCompleted");
        assertEquals(1, lastKnownStatuses.size(), "lastKnownStatuses should have one element after onNext");
    }

    @Test
    void onError() {
        AtomicBoolean streamEnabled = new AtomicBoolean(true);
        ArrayDeque<String> lastKnownStatuses = new ArrayDeque<>();
        final int lastKnownStatusesCapacity = 10;
        PublishStreamObserver publishStreamObserver =
                new PublishStreamObserver(streamEnabled, lastKnownStatuses, lastKnownStatusesCapacity);

        publishStreamObserver.onError(new Throwable());
        assertFalse(streamEnabled.get(), "streamEnabled should be set to false after onError");
        assertEquals(1, lastKnownStatuses.size(), "lastKnownStatuses should have one element after onError");
    }

    @Test
    void onCompleted() {
        AtomicBoolean streamEnabled = new AtomicBoolean(true);
        ArrayDeque<String> lastKnownStatuses = new ArrayDeque<>();
        final int lastKnownStatusesCapacity = 10;
        PublishStreamObserver publishStreamObserver =
                new PublishStreamObserver(streamEnabled, lastKnownStatuses, lastKnownStatusesCapacity);

        publishStreamObserver.onCompleted();
        assertTrue(streamEnabled.get(), "streamEnabled should remain true after onCompleted");
        assertEquals(0, lastKnownStatuses.size(), "lastKnownStatuses should not have elements after onCompleted");
    }
}
