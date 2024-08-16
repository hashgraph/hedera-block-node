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

package com.hedera.block.server.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.hedera.block.server.ServiceStatus;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    private static final String READINESS_PATH = "/readiness";
    private static final String LIVENESS_PATH = "/liveness";
    private static final String HEALTH_PATH = "/healthz";

    @Mock private ServiceStatus serviceStatus;

    @Mock ServerRequest serverRequest;

    @Mock ServerResponse serverResponse;

    @Test
    public void testHandleLiveness() {
        // given
        when(serviceStatus.isRunning()).thenReturn(true);
        when(serverResponse.status(200)).thenReturn(serverResponse);
        doNothing().when(serverResponse).send("OK");
        HealthService healthService = new HealthServiceImpl(serviceStatus);

        // when
        healthService.handleLiveness(serverRequest, serverResponse);

        // then
        verify(serverResponse, times(1)).status(200);
        verify(serverResponse, times(1)).send("OK");
    }

    @Test
    public void testHandleLiveness_notRunning() {
        // given
        when(serviceStatus.isRunning()).thenReturn(false);
        when(serverResponse.status(503)).thenReturn(serverResponse);
        doNothing().when(serverResponse).send("Service is not running");
        HealthService healthService = new HealthServiceImpl(serviceStatus);

        // when
        healthService.handleLiveness(serverRequest, serverResponse);

        // then
        verify(serverResponse, times(1)).status(503);
        verify(serverResponse, times(1)).send("Service is not running");
    }

    @Test
    public void testHandleReadiness() {
        // given
        when(serviceStatus.isRunning()).thenReturn(true);
        when(serverResponse.status(200)).thenReturn(serverResponse);
        doNothing().when(serverResponse).send("OK");
        HealthService healthService = new HealthServiceImpl(serviceStatus);

        // when
        healthService.handleReadiness(serverRequest, serverResponse);

        // then
        verify(serverResponse, times(1)).status(200);
        verify(serverResponse, times(1)).send("OK");
    }

    @Test
    public void testHandleReadiness_notRunning() {
        // given
        when(serviceStatus.isRunning()).thenReturn(false);
        when(serverResponse.status(503)).thenReturn(serverResponse);
        doNothing().when(serverResponse).send("Service is not running");
        HealthService healthService = new HealthServiceImpl(serviceStatus);

        // when
        healthService.handleReadiness(serverRequest, serverResponse);

        // then
        verify(serverResponse, times(1)).status(503);
        verify(serverResponse, times(1)).send("Service is not running");
    }

    @Test
    public void testRouting() {
        // given
        HealthService healthService = new HealthServiceImpl(serviceStatus);
        HttpRules httpRules = mock(HttpRules.class);
        when(httpRules.get(anyString(), any())).thenReturn(httpRules);

        // when
        healthService.routing(httpRules);

        // then
        verify(httpRules, times(1)).get(eq(LIVENESS_PATH), any());
        verify(httpRules, times(1)).get(eq(READINESS_PATH), any());
        assertEquals(HEALTH_PATH, healthService.getHealthRootPath());
    }
}
