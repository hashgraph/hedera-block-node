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

import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SingleBlocksNotFound;
import static com.hedera.block.server.metrics.BlockNodeMetricTypes.Counter.SingleBlocksRetrieved;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.metrics.MetricsService;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.producer.ProducerBlockItemObserver;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.hapi.block.PublishStreamRequest;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SingleBlockRequest;
import com.hedera.hapi.block.SingleBlockResponse;
import com.hedera.hapi.block.SingleBlockResponseCode;
import com.hedera.hapi.block.SubscribeStreamRequest;
import com.hedera.hapi.block.SubscribeStreamResponse;
import com.hedera.hapi.block.stream.Block;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipelines;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.Flow;
import javax.inject.Inject;

public class PbjBlockStreamServiceProxy implements PbjBlockStreamService {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final LiveStreamMediator streamMediator;
    private final ServiceStatus serviceStatus;
    private final BlockReader<Block> blockReader;

    private final BlockNodeContext blockNodeContext;
    private final MetricsService metricsService;

    private final Notifier notifier;

    @Inject
    PbjBlockStreamServiceProxy(
            @NonNull final LiveStreamMediator streamMediator,
            @NonNull final BlockReader<Block> blockReader,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull
                    final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponse>>
                            streamPersistenceHandler,
            @NonNull final Notifier notifier,
            @NonNull final BlockNodeContext blockNodeContext) {
        this.blockReader = blockReader;
        this.serviceStatus = serviceStatus;
        this.notifier = notifier;
        this.blockNodeContext = blockNodeContext;
        this.metricsService = blockNodeContext.metricsService();

        streamMediator.subscribe(streamPersistenceHandler);
        this.streamMediator = streamMediator;
    }

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
                case singleBlock -> Pipelines.<SingleBlockRequest, SingleBlockResponse>unary()
                        .mapRequest(bytes -> parseSingleBlockRequest(bytes, options))
                        .method(this::singleBlock)
                        .mapResponse(reply -> createSingleBlockResponse(reply, options))
                        .respondTo(replies)
                        .build();
                case publishBlockStream -> Pipelines
                        .<PublishStreamRequest, PublishStreamResponse>bidiStreaming()
                        .mapRequest(bytes -> parsePublishStreamRequest(bytes, options))
                        .method(this::publishBlockStream)
                        .mapResponse(reply -> createPublishStreamResponse(reply, options))
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

                case subscribeBlockStream -> null;
            };
        } catch (Exception e) {
            replies.onError(e);
            return Pipelines.noop();
        }
    }

    Flow.Subscriber<? super PublishStreamRequest> publishBlockStream(
            Flow.Subscriber<? super PublishStreamResponse> publishStreamResponseObserver) {
        LOGGER.log(DEBUG, "Executing bidirectional publishBlockStream gRPC method");

        // Unsubscribe any expired notifiers
        notifier.unsubscribeAllExpired();

        final var producerBlockItemObserver =
                new ProducerBlockItemObserver(
                        Clock.systemDefaultZone(),
                        streamMediator,
                        notifier,
                        publishStreamResponseObserver,
                        blockNodeContext,
                        serviceStatus);

        // Register the producer observer with the notifier to publish responses back to the
        // producer
        notifier.subscribe(producerBlockItemObserver);

        return producerBlockItemObserver;
    }

    private SingleBlockResponse singleBlock(SingleBlockRequest singleBlockRequest) {

        LOGGER.log(DEBUG, "Executing Unary singleBlock gRPC method");

        if (serviceStatus.isRunning()) {
            final long blockNumber = singleBlockRequest.blockNumber();
            try {
                final Optional<Block> blockOpt = blockReader.read(blockNumber);
                if (blockOpt.isPresent()) {
                    LOGGER.log(DEBUG, "Successfully returning block number: {0}", blockNumber);
                    metricsService.get(SingleBlocksRetrieved).increment();

                    return SingleBlockResponse.newBuilder()
                            .status(SingleBlockResponseCode.READ_BLOCK_SUCCESS)
                            .build();
                } else {
                    LOGGER.log(DEBUG, "Block number {0} not found", blockNumber);
                    metricsService.get(SingleBlocksNotFound).increment();

                    return SingleBlockResponse.newBuilder()
                            .status(SingleBlockResponseCode.READ_BLOCK_NOT_FOUND)
                            .build();
                }
            } catch (IOException e) {
                LOGGER.log(ERROR, "Error reading block number: {0}", blockNumber);

                return SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                        .build();
            } catch (ParseException e) {
                LOGGER.log(ERROR, "Error parsing block number: {0}", blockNumber);

                return SingleBlockResponse.newBuilder()
                        .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                        .build();
            }
        } else {
            LOGGER.log(ERROR, "Unary singleBlock gRPC method is not currently running");

            return SingleBlockResponse.newBuilder()
                    .status(SingleBlockResponseCode.READ_BLOCK_NOT_AVAILABLE)
                    .build();
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
