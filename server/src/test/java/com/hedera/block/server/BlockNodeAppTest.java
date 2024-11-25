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
    private Notifier notifier;

    @Mock
    private BlockNodeContext blockNodeContext;

    private BlockNodeApp blockNodeApp;

    @BeforeEach
    void setup() {

        blockNodeApp = new BlockNodeApp(
                serviceStatus,
                healthService,
                new PbjBlockStreamServiceProxy(
                        liveStreamMediator, serviceStatus, blockNodeEventHandler, notifier, blockNodeContext),
                new PbjBlockAccessServiceProxy(serviceStatus, blockReader, blockNodeContext),
                webServerBuilder);

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
