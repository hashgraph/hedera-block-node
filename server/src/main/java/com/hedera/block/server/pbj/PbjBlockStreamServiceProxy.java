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

import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.SubscribeStreamRequest;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipelines;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.concurrent.Flow;

public class PbjBlockStreamServiceProxy implements PbjBlockStreamService {

    //    @NonNull
    //    private SingleBlockResponse singleBlock(SingleBlockRequest singleBlockRequest) {
    //        return pbjBlockStreamService.singleBlock(singleBlockRequest);
    //    }
    //
    //    public Flow.Subscriber<? super PublishStreamRequest> publishBlockStream(
    //            Flow.Subscriber<? super PublishStreamResponse> publishStreamRequest) {
    //        return pbjBlockStreamService.publishBlockStream(publishStreamRequest);
    //    }
    //
    //    public void subscribeBlockStream(
    //            SubscribeStreamRequest subscribeStreamRequest,
    //            Flow.Subscriber<? super SubscribeStreamResponse> responses) {
    //        pbjBlockStreamService.subscribeBlockStream(subscribeStreamRequest, responses);
    //    }

    // rpc methods defined in hedera-protobufs/block/block_service.proto
    //    SingleBlockResponse singleBlock(SingleBlockRequest singleBlockRequest);
    //
    //    Flow.Subscriber<? super PublishStreamRequest> publishBlockStream(
    //            Flow.Subscriber<? super PublishStreamResponse> publishStreamRequest);
    //
    //    void subscribeBlockStream(
    //            SubscribeStreamRequest subscribeStreamRequest,
    //            Flow.Subscriber<? super SubscribeStreamResponse> responses);

    @Override
    @NonNull
    public Flow.Subscriber<? super Bytes> open(
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
                    //                case subscribeBlockStream -> Pipelines
                    //                        .<SubscribeStreamRequest,
                    // SubscribeStreamResponse>serverStreaming()
                    //                        .mapRequest(bytes ->
                    // parseSubscribeStreamRequest(bytes, options))
                    //                        .method(this::subscribeBlockStream)
                    //                        .mapResponse(reply ->
                    // createSubscribeStreamResponse(reply, options))
                    //                        .respondTo(replies)
                    //                        .build();
                    // Client and server are sending messages back and forth.
                    //                case publishBlockStream -> Pipelines
                    //                        .<PublishStreamRequest,
                    // PublishStreamResponse>bidiStreaming()
                    //                        .mapRequest(bytes -> parsePublishStreamRequest(bytes,
                    // options))
                    //                        .method(this::publishBlockStream)
                    //                        .mapResponse(reply ->
                    // createPublishStreamResponse(reply, options))
                    //                        .respondTo(replies)
                    //                        .build();
                case publishBlockStream -> null;
                case subscribeBlockStream -> null;
            };
        } catch (Exception e) {
            replies.onError(e);
            return Pipelines.noop();
        }
    }

    private SingleBlockResponse singleBlock(SingleBlockRequest request) {
        return SingleBlockResponse.newBuilder()
                .status(SingleBlockResponseCode.READ_BLOCK_SUCCESS)
                .build();
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
