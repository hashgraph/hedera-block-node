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

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;

public final class Translator {
    private Translator() {}

    @NonNull
    public static com.hedera.hapi.block.protoc.SingleBlockResponse toProtocSingleBlockResponse(
            @NonNull final SingleBlockResponse singleBlockResponse) {
        try {
            @NonNull
            final byte[] pbjBytes =
                    SingleBlockResponse.PROTOBUF.toBytes(singleBlockResponse).toByteArray();
            return com.hedera.hapi.block.protoc.SingleBlockResponse.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static com.hedera.hapi.block.protoc.SingleBlockResponse toProtocSingleBlockResponse(
            @NonNull final Block block) {
        @NonNull
        final SingleBlockResponse singleBlockResponse =
                SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_SUCCESS)
                        .block(block)
                        .build();

        return toProtocSingleBlockResponse(singleBlockResponse);
    }

    @NonNull
    public static com.hedera.hapi.block.protoc.PublishStreamResponse toProtocPublishStreamResponse(
            @NonNull final com.hedera.hapi.block.PublishStreamResponse publishStreamResponse) {
        try {
            @NonNull
            final byte[] pbjBytes =
                    com.hedera.hapi.block.PublishStreamResponse.PROTOBUF
                            .toBytes(publishStreamResponse)
                            .toByteArray();
            return com.hedera.hapi.block.protoc.PublishStreamResponse.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static BlockItem toPbjBlockItem(
            @NonNull final com.hedera.hapi.block.stream.protoc.BlockItem blockItem) {
        try {
            @NonNull final byte[] protocBytes = blockItem.toByteArray();
            @NonNull final Bytes bytes = Bytes.wrap(protocBytes);
            return BlockItem.PROTOBUF.parse(bytes);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static com.hedera.hapi.block.protoc.PublishStreamRequest toProtocPublishStreamRequest(
            @NonNull final com.hedera.hapi.block.PublishStreamRequest publishStreamRequest) {
        try {
            @NonNull
            final byte[] pbjBytes =
                    com.hedera.hapi.block.PublishStreamRequest.PROTOBUF
                            .toBytes(publishStreamRequest)
                            .toByteArray();
            return com.hedera.hapi.block.protoc.PublishStreamRequest.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static com.hedera.hapi.block.protoc.SubscribeStreamResponse
            toProtocSubscribeStreamResponse(
                    @NonNull
                            final com.hedera.hapi.block.SubscribeStreamResponse
                                    subscribeStreamResponse) {
        try {
            byte[] pbjBytes =
                    com.hedera.hapi.block.SubscribeStreamResponse.PROTOBUF
                            .toBytes(subscribeStreamResponse)
                            .toByteArray();
            return com.hedera.hapi.block.protoc.SubscribeStreamResponse.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static com.hedera.hapi.block.protoc.SubscribeStreamRequest
            toProtocSubscribeStreamRequest(
                    @NonNull
                            final com.hedera.hapi.block.SubscribeStreamRequest
                                    subscribeStreamRequest) {
        try {
            byte[] pbjBytes =
                    com.hedera.hapi.block.SubscribeStreamRequest.PROTOBUF
                            .toBytes(subscribeStreamRequest)
                            .toByteArray();
            return com.hedera.hapi.block.protoc.SubscribeStreamRequest.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
