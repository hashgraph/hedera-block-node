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

package com.hedera.block.server.pbj;

import static com.hedera.block.server.Constants.SERVICE_NAME;

import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SubscribeStreamRequest;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipelines;
import com.hedera.pbj.runtime.grpc.ServiceInterface;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Flow;

public interface PbjBlockStreamService extends ServiceInterface {
    enum BlockStreamMethod implements Method {
        singleBlock,
        publishBlockStream,
        subscribeBlockStream
    }

    // rpc methods defined in hedera-protobufs/block/block_service.proto
    SingleBlockResponse singleBlock(SingleBlockRequest singleBlockRequest);

    Flow.Subscriber<? super PublishStreamRequest> publishBlockStream(
            Flow.Subscriber<? super PublishStreamResponse> publishStreamRequest);

    void subscribeBlockStream(
            SubscribeStreamRequest subscribeStreamRequest,
            Flow.Subscriber<? super SubscribeStreamResponse> responses);

    @NonNull
    default String serviceName() {
        return SERVICE_NAME;
    }

    @NonNull
    default String fullName() {
        return "com.hedera.hapi.block." + SERVICE_NAME;
    }

    @NonNull
    default List<Method> methods() {
        return Arrays.asList(BlockStreamMethod.values());
    }

    @Override
    @NonNull
    default Flow.Subscriber<? super Bytes> open(
            final @NonNull Method method,
            final @NonNull RequestOptions options,
            final @NonNull Flow.Subscriber<? super Bytes> replies) {

        final var m = (BlockStreamMethod) method;
        try {
            return switch (m) {
                    // Simple request -> response
                case singleBlock -> Pipelines.<SingleBlockRequest, SingleBlockResponse>unary()
                        .mapRequest(bytes -> parseSingleBlockRequest(bytes, options))
                        .method(this::singleBlock)
                        .mapResponse(reply -> createSingleBlockResponse(reply, options))
                        .respondTo(replies)
                        .build();
                    // Client sends a single request and the server sends a stream of responses
                case subscribeBlockStream -> Pipelines
                        .<SubscribeStreamRequest, SubscribeStreamResponse>serverStreaming()
                        .mapRequest(bytes -> parseSubscribeStreamRequest(bytes, options))
                        .method(this::subscribeBlockStream)
                        .mapResponse(reply -> createSubscribeStreamResponse(reply, options))
                        .respondTo(replies)
                        .build();
                    // Client and server are sending messages back and forth.
                case publishBlockStream -> Pipelines
                        .<PublishStreamRequest, PublishStreamResponse>bidiStreaming()
                        .mapRequest(bytes -> parsePublishStreamRequest(bytes, options))
                        .method(this::publishBlockStream)
                        .mapResponse(reply -> createPublishStreamResponse(reply, options))
                        .respondTo(replies)
                        .build();
            };
        } catch (Exception e) {
            replies.onError(e);
            return Pipelines.noop();
        }
    }

    @NonNull
    private SingleBlockRequest parseSingleBlockRequest(
            @NonNull final Bytes message, @NonNull final RequestOptions options)
            throws ParseException {
        return SingleBlockRequest.PROTOBUF.parse(message);
    }

    @NonNull
    private Bytes createSingleBlockResponse(
            @NonNull final SingleBlockResponse reply, @NonNull final RequestOptions options) {
        return SingleBlockResponse.PROTOBUF.toBytes(reply);
    }

    @NonNull
    private SubscribeStreamRequest parseSubscribeStreamRequest(
            @NonNull final Bytes message, @NonNull final RequestOptions options)
            throws ParseException {
        return SubscribeStreamRequest.PROTOBUF.parse(message);
    }

    @NonNull
    private Bytes createSubscribeStreamResponse(
            @NonNull final SubscribeStreamResponse subscribeStreamResponse,
            @NonNull final RequestOptions options) {

        return SubscribeStreamResponse.PROTOBUF.toBytes(subscribeStreamResponse);
    }

    @NonNull
    private PublishStreamRequest parsePublishStreamRequest(
            @NonNull final Bytes message, @NonNull final RequestOptions options)
            throws ParseException {

        return PublishStreamRequest.PROTOBUF.parse(message);
    }

    @NonNull
    private Bytes createPublishStreamResponse(
            @NonNull final PublishStreamResponse publishStreamResponse,
            @NonNull final RequestOptions options) {
        return PublishStreamResponse.PROTOBUF.toBytes(publishStreamResponse);
    }
}
