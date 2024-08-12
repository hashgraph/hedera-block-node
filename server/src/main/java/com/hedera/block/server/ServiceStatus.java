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

/**
 * The ServiceStatus interface defines the contract for checking the status of the service and
 * shutting down the web server.
 */
public interface ServiceStatus {

    /**
     * Checks if the service is running.
     *
     * @return true if the service is running, false otherwise
     */
    boolean isRunning();

    /**
     * Sets the running status of the service.
     *
     * @param running true if the service is running, false otherwise
     */
    void setRunning(final boolean running);

    /**
     * Sets the web server instance.
     *
     * @param webServer the web server instance
     */
    void setWebServer(@NonNull final WebServer webServer);

    /**
     * Stops the service and web server. This method is called to shut down the service and the web
     * server in the event of an error or when the service needs to restart.
     */
    void stopWebServer();
}
