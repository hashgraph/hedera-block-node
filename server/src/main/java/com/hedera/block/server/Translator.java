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

import static java.util.Objects.requireNonNull;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.SubscribeStreamRequest;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.pbj.runtime.Codec;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.hedera.pbj.runtime.io.stream.WritableStreamingData;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * TODO: Remove this class once the Helidon PBJ gRPC work is integrated. Translator class to convert
 * between PBJ and google protoc objects.
 */
public final class Translator {
    private static final System.Logger LOGGER = System.getLogger(Translator.class.getName());

    private Translator() {}

    /**
     * TODO: Remove this method once the Helidon PBJ gRPC work is integrated. Converts a {@link
     * SingleBlockResponse} to a {@link com.hedera.hapi.block.protoc.SingleBlockResponse}.
     *
     * @param singleBlockResponse the {@link SingleBlockResponse} to convert
     * @return the converted {@link com.hedera.hapi.block.protoc.SingleBlockResponse}
     */
    @NonNull
    public static com.hedera.hapi.block.protoc.SingleBlockResponse toProtocSingleBlockResponse(
            @NonNull final SingleBlockResponse singleBlockResponse) {
        try {
            final byte[] pbjBytes = asBytes(SingleBlockResponse.PROTOBUF, singleBlockResponse);
            return com.hedera.hapi.block.protoc.SingleBlockResponse.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "Error converting SingleBlockResponse to protoc SingleBlockResponse for: {0}",
                    singleBlockResponse);
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: Remove this method once the Helidon PBJ gRPC work is integrated. Converts a {@link
     * Block} to a {@link com.hedera.hapi.block.protoc.SingleBlockResponse}.
     *
     * @param block the {@link Block} to convert to a protoc single block response.
     * @return an instance of {@link com.hedera.hapi.block.protoc.SingleBlockResponse}
     */
    @NonNull
    public static com.hedera.hapi.block.protoc.SingleBlockResponse toProtocSingleBlockResponse(
            @NonNull final Block block) {
        final SingleBlockResponse singleBlockResponse =
                SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_SUCCESS)
                        .block(block)
                        .build();

        return toProtocSingleBlockResponse(singleBlockResponse);
    }

    /**
     * TODO: Remove this method once the Helidon PBJ gRPC work is integrated. Converts a {@link
     * com.hedera.hapi.block.PublishStreamResponse} to a {@link
     * com.hedera.hapi.block.protoc.PublishStreamResponse}.
     *
     * @param publishStreamResponse the {@link com.hedera.hapi.block.PublishStreamResponse} to
     *     convert
     * @return the converted {@link com.hedera.hapi.block.protoc.PublishStreamResponse}
     */
    @NonNull
    public static com.hedera.hapi.block.protoc.PublishStreamResponse toProtocPublishStreamResponse(
            @NonNull final com.hedera.hapi.block.PublishStreamResponse publishStreamResponse) {
        try {
            final byte[] pbjBytes = asBytes(PublishStreamResponse.PROTOBUF, publishStreamResponse);
            return com.hedera.hapi.block.protoc.PublishStreamResponse.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "Error converting PublishStreamResponse to protoc PublishStreamResponse for:"
                            + " {0}",
                    publishStreamResponse);
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: Remove this method once the Helidon PBJ gRPC work is integrated. Converts a {@link
     * com.hedera.hapi.block.PublishStreamRequest} to a {@link
     * com.hedera.hapi.block.protoc.PublishStreamRequest}.
     *
     * @param publishStreamRequest the {@link com.hedera.hapi.block.PublishStreamRequest} to convert
     * @return the converted {@link com.hedera.hapi.block.protoc.PublishStreamRequest}
     */
    @NonNull
    public static com.hedera.hapi.block.protoc.PublishStreamRequest toProtocPublishStreamRequest(
            @NonNull final com.hedera.hapi.block.PublishStreamRequest publishStreamRequest) {
        try {
            final byte[] pbjBytes = asBytes(PublishStreamRequest.PROTOBUF, publishStreamRequest);
            return com.hedera.hapi.block.protoc.PublishStreamRequest.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "Error converting PublishStreamRequest to protoc PublishStreamRequest for: {0}",
                    publishStreamRequest);
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: Remove this method once the Helidon PBJ gRPC work is integrated. Converts a {@link
     * com.hedera.hapi.block.SubscribeStreamResponse} to a {@link
     * com.hedera.hapi.block.protoc.SubscribeStreamResponse}.
     *
     * @param subscribeStreamResponse the {@link com.hedera.hapi.block.SubscribeStreamResponse} to
     *     convert
     * @return the converted {@link com.hedera.hapi.block.protoc.SubscribeStreamResponse}
     */
    @NonNull
    public static com.hedera.hapi.block.protoc.SubscribeStreamResponse
            toProtocSubscribeStreamResponse(
                    @NonNull
                            final com.hedera.hapi.block.SubscribeStreamResponse
                                    subscribeStreamResponse) {
        try {
            final byte[] pbjBytes =
                    asBytes(SubscribeStreamResponse.PROTOBUF, subscribeStreamResponse);
            return com.hedera.hapi.block.protoc.SubscribeStreamResponse.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "Error converting SubscribeStreamResponse to protoc SubscribeStreamResponse"
                            + " for: {0}",
                    subscribeStreamResponse);
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: Remove this method once the Helidon PBJ gRPC work is integrated. Converts a {@link
     * com.hedera.hapi.block.SubscribeStreamRequest} to a {@link
     * com.hedera.hapi.block.protoc.SubscribeStreamRequest}.
     *
     * @param subscribeStreamRequest the {@link com.hedera.hapi.block.SubscribeStreamRequest} to
     *     convert
     * @return the converted {@link com.hedera.hapi.block.protoc.SubscribeStreamRequest}
     */
    @NonNull
    public static com.hedera.hapi.block.protoc.SubscribeStreamRequest
            toProtocSubscribeStreamRequest(
                    @NonNull
                            final com.hedera.hapi.block.SubscribeStreamRequest
                                    subscribeStreamRequest) {
        try {
            final byte[] pbjBytes =
                    asBytes(SubscribeStreamRequest.PROTOBUF, subscribeStreamRequest);
            return com.hedera.hapi.block.protoc.SubscribeStreamRequest.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "Error converting SubscribeStreamRequest to protoc SubscribeStreamRequest for:"
                            + " {0}",
                    subscribeStreamRequest);
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: Remove this method once the Helidon PBJ gRPC work is integrated. Converts a {@link
     * com.hedera.hapi.block.stream.protoc.BlockItem} to a {@link
     * com.hedera.hapi.block.stream.BlockItem}.
     *
     * @param blockItem the {@link com.hedera.hapi.block.stream.protoc.BlockItem} to convert
     * @return the converted {@link com.hedera.hapi.block.stream.BlockItem}
     * @throws ParseException if the converstion between the protoc and PBJ objects fails
     */
    @NonNull
    public static BlockItem toPbjBlockItem(
            @NonNull final com.hedera.hapi.block.stream.protoc.BlockItem blockItem)
            throws ParseException {
        return BlockItem.PROTOBUF.parse(Bytes.wrap(blockItem.toByteArray()));
    }

    private static <T extends Record> byte[] asBytes(@NonNull Codec<T> codec, @NonNull T tx) {
        requireNonNull(codec);
        requireNonNull(tx);
        try {
            final var bytes = new ByteArrayOutputStream();
            codec.write(tx, new WritableStreamingData(bytes));
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unable to convert from PBJ to bytes", e);
        }
    }
}
