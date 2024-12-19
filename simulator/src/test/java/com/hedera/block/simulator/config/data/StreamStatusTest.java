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

package com.hedera.block.simulator.config.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreamStatusTest {

    @Test
    void testBuilderDefaultValues() {
        StreamStatus streamStatus = StreamStatus.builder().build();

        assertEquals(0, streamStatus.publishedBlocks(), "Default publishedBlocks should be 0");
        assertEquals(0, streamStatus.consumedBlocks(), "Default consumedBlocks should be 0");
        assertNotNull(streamStatus.lastKnownPublisherStatuses(), "lastKnownPublisherStatuses should not be null");
        assertTrue(streamStatus.lastKnownPublisherStatuses().isEmpty(), "lastKnownPublisherStatuses should be empty");
        assertNotNull(streamStatus.lastKnownConsumersStatuses(), "lastKnownConsumersStatuses should not be null");
        assertTrue(streamStatus.lastKnownConsumersStatuses().isEmpty(), "lastKnownConsumersStatuses should be empty");
    }

    @Test
    void testBuilderWithValues() {
        List<String> publisherStatuses = List.of("Publisher1", "Publisher2");
        List<String> consumerStatuses = List.of("Consumer1");

        StreamStatus streamStatus = StreamStatus.builder()
                .publishedBlocks(10)
                .consumedBlocks(8)
                .lastKnownPublisherStatuses(publisherStatuses)
                .lastKnownConsumersStatuses(consumerStatuses)
                .build();

        assertEquals(10, streamStatus.publishedBlocks(), "publishedBlocks should be 10");
        assertEquals(8, streamStatus.consumedBlocks(), "consumedBlocks should be 8");
        assertIterableEquals(
                publisherStatuses,
                streamStatus.lastKnownPublisherStatuses(),
                "lastKnownPublisherStatuses should match");
        assertIterableEquals(
                consumerStatuses, streamStatus.lastKnownConsumersStatuses(), "lastKnownConsumersStatuses should match");
    }

    @Test
    void testBuilderSetters() {
        StreamStatus.Builder builder = StreamStatus.builder();

        builder.publishedBlocks(5);
        builder.consumedBlocks(3);
        builder.lastKnownPublisherStatuses(List.of("PubStatus"));
        builder.lastKnownConsumersStatuses(List.of("ConStatus"));

        StreamStatus streamStatus = builder.build();

        assertEquals(5, streamStatus.publishedBlocks(), "publishedBlocks should be 5");
        assertEquals(3, streamStatus.consumedBlocks(), "consumedBlocks should be 3");
        assertIterableEquals(
                List.of("PubStatus"),
                streamStatus.lastKnownPublisherStatuses(),
                "lastKnownPublisherStatuses should match");
        assertIterableEquals(
                List.of("ConStatus"),
                streamStatus.lastKnownConsumersStatuses(),
                "lastKnownConsumersStatuses should match");
    }

    @Test
    void testBuilderDefaultConstructor() {
        StreamStatus.Builder builder = new StreamStatus.Builder();

        assertNotNull(builder, "Builder should not be null");

        StreamStatus streamStatus = builder.build();

        assertEquals(0, streamStatus.publishedBlocks(), "Default publishedBlocks should be 0");
        assertEquals(0, streamStatus.consumedBlocks(), "Default consumedBlocks should be 0");
        assertNotNull(streamStatus.lastKnownPublisherStatuses(), "lastKnownPublisherStatuses should not be null");
        assertTrue(streamStatus.lastKnownPublisherStatuses().isEmpty(), "lastKnownPublisherStatuses should be empty");
        assertNotNull(streamStatus.lastKnownConsumersStatuses(), "lastKnownConsumersStatuses should not be null");
        assertTrue(streamStatus.lastKnownConsumersStatuses().isEmpty(), "lastKnownConsumersStatuses should be empty");
    }

    @Test
    void testEqualsAndHashCode() {
        List<String> publisherStatuses = List.of("Publisher1");
        List<String> consumerStatuses = List.of("Consumer1");

        StreamStatus streamStatus1 = StreamStatus.builder()
                .publishedBlocks(5)
                .consumedBlocks(3)
                .lastKnownPublisherStatuses(publisherStatuses)
                .lastKnownConsumersStatuses(consumerStatuses)
                .build();

        StreamStatus streamStatus2 = StreamStatus.builder()
                .publishedBlocks(5)
                .consumedBlocks(3)
                .lastKnownPublisherStatuses(publisherStatuses)
                .lastKnownConsumersStatuses(consumerStatuses)
                .build();

        assertIterableEquals(
                streamStatus1.lastKnownPublisherStatuses(),
                streamStatus2.lastKnownPublisherStatuses(),
                "lastKnownPublisherStatuses should match");
        assertIterableEquals(
                streamStatus1.lastKnownConsumersStatuses(),
                streamStatus2.lastKnownConsumersStatuses(),
                "lastKnownConsumersStatuses should match");
    }

    @Test
    void testNotEquals() {
        StreamStatus streamStatus1 =
                StreamStatus.builder().publishedBlocks(5).consumedBlocks(3).build();

        StreamStatus streamStatus2 =
                StreamStatus.builder().publishedBlocks(6).consumedBlocks(3).build();

        assertNotEquals(streamStatus1, streamStatus2, "StreamStatus instances should not be equal");
    }

    @Test
    void testToString() {
        List<String> publisherStatuses = List.of("Pub1");
        List<String> consumerStatuses = List.of("Con1");

        StreamStatus streamStatus = StreamStatus.builder()
                .publishedBlocks(5)
                .consumedBlocks(3)
                .lastKnownPublisherStatuses(publisherStatuses)
                .lastKnownConsumersStatuses(consumerStatuses)
                .build();

        String toString = streamStatus.toString();

        assertNotNull(toString, "toString() should not return null");
        assertTrue(toString.contains("publishedBlocks=5"), "toString() should contain 'publishedBlocks=5'");
        assertTrue(toString.contains("consumedBlocks=3"), "toString() should contain 'consumedBlocks=3'");
        assertTrue(
                toString.contains("lastKnownPublisherStatuses=[Pub1]"),
                "toString() should contain 'lastKnownPublisherStatuses=[Pub1]'");
        assertTrue(
                toString.contains("lastKnownConsumersStatuses=[Con1]"),
                "toString() should contain 'lastKnownConsumersStatuses=[Con1]'");
    }

    @Test
    void testStatusesLists() {
        List<String> publisherStatuses = new ArrayList<>();
        List<String> consumerStatuses = new ArrayList<>();

        publisherStatuses.add("Publisher1");
        consumerStatuses.add("Consumer1");

        StreamStatus streamStatus = StreamStatus.builder()
                .publishedBlocks(1)
                .consumedBlocks(1)
                .lastKnownPublisherStatuses(publisherStatuses)
                .lastKnownConsumersStatuses(consumerStatuses)
                .build();

        publisherStatuses.add("Publisher2");
        consumerStatuses.add("Consumer2");

        assertNotEquals(
                List.of("Publisher1", "Publisher2"),
                streamStatus.lastKnownPublisherStatuses(),
                "lastKnownPublisherStatuses should be immutable");
        assertNotEquals(
                List.of("Consumer1", "Consumer2"),
                streamStatus.lastKnownConsumersStatuses(),
                "lastKnownConsumersStatuses should be immutable");
    }

    @Test
    void testNullLists() {
        assertThrows(NullPointerException.class, () -> StreamStatus.builder()
                .publishedBlocks(0)
                .consumedBlocks(0)
                .lastKnownPublisherStatuses(null)
                .lastKnownConsumersStatuses(null)
                .build());
    }

    @Test
    void testBuilderChaining() {
        StreamStatus streamStatus = StreamStatus.builder()
                .publishedBlocks(2)
                .consumedBlocks(2)
                .lastKnownPublisherStatuses(List.of("PubStatus"))
                .lastKnownConsumersStatuses(List.of("ConStatus"))
                .build();

        assertEquals(2, streamStatus.publishedBlocks(), "publishedBlocks should be 2");
        assertEquals(2, streamStatus.consumedBlocks(), "consumedBlocks should be 2");
        assertIterableEquals(
                List.of("PubStatus"),
                streamStatus.lastKnownPublisherStatuses(),
                "lastKnownPublisherStatuses should match");
        assertIterableEquals(
                List.of("ConStatus"),
                streamStatus.lastKnownConsumersStatuses(),
                "lastKnownConsumersStatuses should match");
    }
}
