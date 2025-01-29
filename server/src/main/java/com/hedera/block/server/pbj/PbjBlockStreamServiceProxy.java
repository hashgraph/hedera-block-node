// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.pbj;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.consumer.LiveStreamEventHandlerBuilder;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.producer.NoOpProducerObserver;
import com.hedera.block.server.producer.ProducerBlockItemObserver;
import com.hedera.block.server.producer.ProducerConfig;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.verification.StreamVerificationHandlerImpl;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.PublishStreamRequestUnparsed;
import com.hedera.hapi.block.PublishStreamResponse;
import com.hedera.hapi.block.SubscribeStreamRequest;
import com.hedera.hapi.block.SubscribeStreamResponseCode;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.grpc.Pipeline;
import com.hedera.pbj.runtime.grpc.Pipelines;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

/**
 * PbjBlockStreamServiceProxy is the runtime binding between the PBJ Helidon Plugin and the
 * Block Node. The Helidon Plugin routes inbound requests to this class based on the methods
 * and service names in PbjBlockStreamService. Service implementations are instantiated via
 * the open method thereby bridging the client requests into the Block Node application.
 */
public class PbjBlockStreamServiceProxy implements PbjBlockStreamService {

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final ExecutorService executorService;
    private final LiveStreamMediator streamMediator;
    private final ServiceStatus serviceStatus;
    private final BlockNodeContext blockNodeContext;
    private final Notifier notifier;

    /**
     * Creates a new PbjBlockStreamServiceProxy instance.
     *
     * @param streamMediator the live stream mediator
     * @param serviceStatus the service status
     * @param streamPersistenceHandler the stream persistence handler
     * @param streamVerificationHandler the stream verification handler
     * @param notifier the notifier
     * @param blockNodeContext the block node context
     */
    @Inject
    public PbjBlockStreamServiceProxy(
            @NonNull final LiveStreamMediator streamMediator,
            @NonNull final ServiceStatus serviceStatus,
            @NonNull final BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> streamPersistenceHandler,
            @NonNull final StreamVerificationHandlerImpl streamVerificationHandler,
            @NonNull final Notifier notifier,
            @NonNull final BlockNodeContext blockNodeContext) {

        this.serviceStatus = Objects.requireNonNull(serviceStatus);
        this.notifier = Objects.requireNonNull(notifier);
        this.blockNodeContext = Objects.requireNonNull(blockNodeContext);
        streamMediator.subscribe(Objects.requireNonNull(streamPersistenceHandler));
        streamMediator.subscribe(Objects.requireNonNull(streamVerificationHandler));
        this.streamMediator = Objects.requireNonNull(streamMediator);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Pipeline<? super Bytes> open(
            final @NonNull Method method,
            final @NonNull RequestOptions options,
            final @NonNull Pipeline<? super Bytes> replies) {

        final var m = (BlockStreamMethod) method;
        try {
            return switch (m) {
                case publishBlockStream -> {
                    notifier.unsubscribeAllExpired();
                    yield Pipelines.<List<BlockItemUnparsed>, PublishStreamResponse>bidiStreaming()
                            .mapRequest(bytes -> parsePublishStreamRequest(bytes, options))
                            .method(this::publishBlockStream)
                            .mapResponse(bytes -> createPublishStreamResponse(bytes, options))
                            .respondTo(replies)
                            .build();
                }
                case subscribeBlockStream -> Pipelines
                        .<SubscribeStreamRequest, SubscribeStreamResponseUnparsed>serverStreaming()
                        .mapRequest(bytes -> parseSubscribeStreamRequest(bytes, options))
                        .method(this::subscribeBlockStream)
                        .mapResponse(reply -> createSubscribeStreamResponse(reply, options))
                        .respondTo(replies)
                        .build();
            };
        } catch (Exception e) {
            replies.onError(e);
            return Pipelines.noop();
        }
    }

    /**
     * Publishes the block stream.
     *
     * @param helidonProducerObserver the helidon producer observer
     * @return the pipeline
     */
    Pipeline<List<BlockItemUnparsed>> publishBlockStream(
            Pipeline<? super PublishStreamResponse> helidonProducerObserver) {
        LOGGER.log(DEBUG, "Executing bidirectional publishBlockStream gRPC method");

        // Unsubscribe any expired notifiers
        notifier.unsubscribeAllExpired();

        final ProducerConfig.ProducerType producerType = blockNodeContext
                .configuration()
                .getConfigData(ProducerConfig.class)
                .type();

        if (producerType == ProducerConfig.ProducerType.NO_OP) {
            return new NoOpProducerObserver(helidonProducerObserver, blockNodeContext);
        }

        final var producerBlockItemObserver = new ProducerBlockItemObserver(
                Clock.systemDefaultZone(),
                streamMediator,
                notifier,
                helidonProducerObserver,
                blockNodeContext,
                serviceStatus);

        if (serviceStatus.isRunning()) {
            // Register the producer observer with the notifier to publish responses back to the
            // producer
            notifier.subscribe(producerBlockItemObserver);
        }

        return producerBlockItemObserver;
    }

    /**
     * Subscribes to the block stream.
     *
     * @param subscribeStreamRequest the subscribe stream request
     * @param subscribeStreamResponseObserver the subscribe stream response observer
     */
    void subscribeBlockStream(
            SubscribeStreamRequest subscribeStreamRequest,
            Pipeline<? super SubscribeStreamResponseUnparsed> subscribeStreamResponseObserver) {

        LOGGER.log(DEBUG, "Executing Server Streaming subscribeBlockStream gRPC method");

        if (serviceStatus.isRunning()) {
            // Unsubscribe any expired notifiers
            streamMediator.unsubscribeAllExpired();
            final var liveStreamEventHandler = LiveStreamEventHandlerBuilder.build(
                    executorService,
                    Clock.systemDefaultZone(),
                    streamMediator,
                    subscribeStreamResponseObserver,
                    blockNodeContext);
            streamMediator.subscribe(liveStreamEventHandler);
        } else {
            LOGGER.log(ERROR, "Server Streaming subscribeBlockStream gRPC Service is not currently running");

            subscribeStreamResponseObserver.onNext(SubscribeStreamResponseUnparsed.newBuilder()
                    .status(SubscribeStreamResponseCode.READ_STREAM_SUCCESS)
                    .build());
        }
    }

    @NonNull
    private SubscribeStreamRequest parseSubscribeStreamRequest(
            @NonNull final Bytes message, @NonNull final RequestOptions options) throws ParseException {
        return SubscribeStreamRequest.PROTOBUF.parse(message);
    }

    @NonNull
    private Bytes createSubscribeStreamResponse(
            @NonNull final SubscribeStreamResponseUnparsed subscribeStreamResponse,
            @NonNull final RequestOptions options) {
        return SubscribeStreamResponseUnparsed.PROTOBUF.toBytes(subscribeStreamResponse);
    }

    @NonNull
    private List<BlockItemUnparsed> parsePublishStreamRequest(
            @NonNull final Bytes message, @NonNull final RequestOptions options) throws ParseException {
        final PublishStreamRequestUnparsed request = PublishStreamRequestUnparsed.PROTOBUF.parse(message);
        return request.blockItems().blockItems();
    }

    @NonNull
    private Bytes createPublishStreamResponse(
            @NonNull final PublishStreamResponse publishStreamResponse, @NonNull final RequestOptions options) {
        return PublishStreamResponse.PROTOBUF.toBytes(publishStreamResponse);
    }
}
