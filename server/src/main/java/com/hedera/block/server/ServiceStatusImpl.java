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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.helidon.webserver.WebServer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The ServiceStatusImpl class implements the ServiceStatus interface. It provides the
 * implementation for checking the status of the service and shutting down the web server.
 */
@Singleton
public class ServiceStatusImpl implements ServiceStatus {

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private WebServer webServer;

    /** Constructor for the ServiceStatusImpl class. */
    @Inject
    public ServiceStatusImpl() {}

    /**
     * Checks if the service is running.
     *
     * @return true if the service is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Sets the running status of the service.
     *
     * @param running true if the service is running, false otherwise
     */
    public void setRunning(final boolean running) {
        isRunning.set(running);
    }

    /**
     * Sets the web server instance.
     *
     * @param webServer the web server instance
     */
    public void setWebServer(@NonNull final WebServer webServer) {
        this.webServer = webServer;
    }

    /**
     * Stops the service and web server. This method is called to shut down the service and the web
     * server in the event of an unrecoverable exception or during expected maintenance.
     */
    public void stopWebServer() {

        // Flag the service to stop
        // accepting new connections
        isRunning.set(false);

        // Stop the web server
        webServer.stop();
    }
}
