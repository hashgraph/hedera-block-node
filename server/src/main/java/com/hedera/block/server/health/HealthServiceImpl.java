// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.health;

import com.hedera.block.server.service.ServiceStatus;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Provides implementation for the health endpoints of the server. */
@Singleton
public class HealthServiceImpl implements HealthService {

    private static final String LIVEZ_PATH = "/livez";
    private static final String READYZ_PATH = "/readyz";

    private final ServiceStatus serviceStatus;

    /**
     * It initializes the HealthService with needed dependencies.
     *
     * @param serviceStatus is used to check the status of the service
     */
    @Inject
    public HealthServiceImpl(@NonNull ServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    @Override
    @NonNull
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
        httpRules.get(LIVEZ_PATH, this::handleLivez).get(READYZ_PATH, this::handleReadyz);
    }

    /**
     * Handles the request for liveness endpoint, that it most be defined on routing implementation.
     *
     * @param req the server request
     * @param res the server response
     */
    @Override
    public final void handleLivez(
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
    public final void handleReadyz(
            @NonNull final ServerRequest req, @NonNull final ServerResponse res) {
        if (serviceStatus.isRunning()) {
            res.status(200).send("OK");
        } else {
            res.status(503).send("Service is not running");
        }
    }
}
