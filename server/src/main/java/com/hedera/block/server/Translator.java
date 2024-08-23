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

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.ERROR;
import static java.util.Objects.requireNonNull;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SubscribeStreamRequest;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.pbj.runtime.Codec;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.hedera.pbj.runtime.io.stream.WritableStreamingData;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Translator class to convert between PBJ and google protoc objects.
 *
 * <p>TODO: Remove this class once the Helidon PBJ gRPC work is integrated.
 */
public final class Translator {
    private static final Logger LOGGER = System.getLogger(Translator.class.getName());

    private static final String INVALID_BUFFER_MESSAGE =
            "Invalid protocol buffer converting %s from PBJ to protoc for %s";

    private Translator() {}

    /**
     * Converts a {@link SingleBlockResponse} to a {@link
     * com.hedera.hapi.block.protoc.SingleBlockResponse}.
     *
     * @param singleBlockResponse the {@link SingleBlockResponse} to convert
     * @return the converted {@link com.hedera.hapi.block.protoc.SingleBlockResponse}
     */
    @NonNull
    public static com.hedera.hapi.block.protoc.SingleBlockResponse fromPbj(
            @NonNull final SingleBlockResponse singleBlockResponse) {
        try {
            final byte[] pbjBytes = asBytes(SingleBlockResponse.PROTOBUF, singleBlockResponse);
            return com.hedera.hapi.block.protoc.SingleBlockResponse.parseFrom(pbjBytes);
        } catch (InvalidProtocolBufferException e) {
            final String message =
                    INVALID_BUFFER_MESSAGE.formatted("SingleBlockResponse", singleBlockResponse);
            LOGGER.log(ERROR, message);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Converts a {@link com.hedera.hapi.block.PublishStreamResponse} to a {@link
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
            final String message =
                    INVALID_BUFFER_MESSAGE.formatted(
                            "PublishStreamResponse", publishStreamResponse);
            LOGGER.log(ERROR, message);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Converts a {@link com.hedera.hapi.block.PublishStreamRequest} to a {@link
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
            final String message =
                    INVALID_BUFFER_MESSAGE.formatted("PublishStreamRequest", publishStreamRequest);
            LOGGER.log(ERROR, message);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Converts a {@link com.hedera.hapi.block.SubscribeStreamResponse} to a {@link
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
            final String message =
                    INVALID_BUFFER_MESSAGE.formatted(
                            "SubscribeStreamResponse", subscribeStreamResponse);
            LOGGER.log(ERROR, message);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Converts a {@link com.hedera.hapi.block.SubscribeStreamRequest} to a {@link
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
            final String message =
                    INVALID_BUFFER_MESSAGE.formatted(
                            "SubscribeStreamRequest", subscribeStreamRequest);
            LOGGER.log(ERROR, message);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Converts protoc bytes to a PBJ record of the same type.
     *
     * @param <T> the type of PBJ record to convert to
     * @param codec the record codec to convert the bytes to a PBJ record
     * @param bytes the protoc bytes to convert to a PBJ record
     * @return the converted PBJ record
     * @throws ParseException if the conversion between the protoc bytes and PBJ objects fails
     */
    @NonNull
    public static <T extends Record> T toPbj(
            @NonNull final Codec<T> codec, @NonNull final byte[] bytes) throws ParseException {
        return codec.parse(Bytes.wrap(bytes));
    }

    @NonNull
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
