// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hedera.block.server.config.BlockNodeContext;
import com.hedera.block.server.events.BlockNodeEventHandler;
import com.hedera.block.server.events.ObjectEvent;
import com.hedera.block.server.health.HealthService;
import com.hedera.block.server.mediator.LiveStreamMediator;
import com.hedera.block.server.notifier.Notifier;
import com.hedera.block.server.pbj.PbjBlockAccessServiceProxy;
import com.hedera.block.server.pbj.PbjBlockStreamServiceProxy;
import com.hedera.block.server.persistence.storage.read.BlockReader;
import com.hedera.block.server.service.ServiceStatus;
import com.hedera.block.server.verification.StreamVerificationHandlerImpl;
import com.hedera.hapi.block.BlockUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import com.hedera.pbj.grpc.helidon.PbjRouting;
import com.hedera.pbj.grpc.helidon.config.PbjConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.http.HttpRouting;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockNodeAppTest {

    @Mock
    private ServiceStatus serviceStatus;

    @Mock
    private HealthService healthService;

    @Mock
    private WebServerConfig.Builder webServerBuilder;

    @Mock
    private WebServer webServer;

    @Mock
    private LiveStreamMediator liveStreamMediator;

    @Mock
    private BlockReader<BlockUnparsed> blockReader;

    @Mock
    private BlockNodeEventHandler<ObjectEvent<SubscribeStreamResponseUnparsed>> blockNodeEventHandler;

    @Mock
    private StreamVerificationHandlerImpl streamVerificationHandler;

    @Mock
    private Notifier notifier;

    @Mock
    private BlockNodeContext blockNodeContext;

    ServerConfig serverConfig;

    private BlockNodeApp blockNodeApp;

    @BeforeEach
    void setup() {

        serverConfig = new ServerConfig(4_194_304, 8080);

        blockNodeApp = new BlockNodeApp(
                serviceStatus,
                healthService,
                new PbjBlockStreamServiceProxy(
                        liveStreamMediator,
                        serviceStatus,
                        blockNodeEventHandler,
                        streamVerificationHandler,
                        notifier,
                        blockNodeContext),
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext),
                webServerBuilder,
                serverConfig);

        when(webServerBuilder.port(8080)).thenReturn(webServerBuilder);
        when(webServerBuilder.addProtocol(any(PbjConfig.class))).thenReturn(webServerBuilder);
        when(webServerBuilder.addRouting(any(PbjRouting.Builder.class))).thenReturn(webServerBuilder);
        when(webServerBuilder.addRouting(any(HttpRouting.Builder.class))).thenReturn(webServerBuilder);
        when(webServerBuilder.build()).thenReturn(webServer);
        when(healthService.getHealthRootPath()).thenReturn("/health");
    }

    @Test
    void testStartServer() throws IOException {
        // Act
        blockNodeApp.start();

        // Assert
        verify(serviceStatus).setWebServer(webServer);
        verify(webServer).start();
        verify(healthService).getHealthRootPath();
        verify(webServerBuilder).port(8080);
        verify(webServerBuilder).addRouting(any(PbjRouting.Builder.class));
        verify(webServerBuilder).addRouting(any(HttpRouting.Builder.class));
        verify(webServerBuilder).addProtocol(any(PbjConfig.class));
        verify(webServerBuilder).build();
    }
}
