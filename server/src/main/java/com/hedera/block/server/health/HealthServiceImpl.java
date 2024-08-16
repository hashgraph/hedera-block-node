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

import com.hedera.block.server.ServiceStatus;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/** Provides implementation for the health endpoints of the server. */
public class HealthServiceImpl implements HealthService {

    private static final String LIVENESS_PATH = "/liveness";
    private static final String READINESS_PATH = "/readiness";

    private final ServiceStatus serviceStatus;

    /**
     * It initializes the HealthService with needed dependencies.
     *
     * @param serviceStatus is used to check the status of the service
     */
    public HealthServiceImpl(@NonNull ServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    @Override
    public String getHealthRootPath() {
        return "/healthz";
    }

    /**
     * Configures the health routes for the server.
     *
     * @param httpRules is used to configure the health endpoints routes
     */
    @Override
    public void routing(@NonNull final HttpRules httpRules) {
        httpRules
                .get(LIVENESS_PATH, this::handleLiveness)
                .get(READINESS_PATH, this::handleReadiness);
    }

    /**
     * Handles the request for liveness endpoint, that it most be defined on routing implementation.
     *
     * @param req the server request
     * @param res the server response
     */
    @Override
    public final void handleLiveness(
            @NonNull final ServerRequest req, @NonNull final ServerResponse res) {
        if (serviceStatus.isRunning()) {
            res.status(200).send("OK");
        } else {
            res.status(503).send("Service is not running");
        }
    }

    /**
     * Handles the request for readiness endpoint, that it most be defined on routing
     * implementation.
     *
     * @param req the server request
     * @param res the server response
     */
    @Override
    public final void handleReadiness(
            @NonNull final ServerRequest req, @NonNull final ServerResponse res) {
        if (serviceStatus.isRunning()) {
            res.status(200).send("OK");
        } else {
            res.status(503).send("Service is not running");
        }
    }
}
