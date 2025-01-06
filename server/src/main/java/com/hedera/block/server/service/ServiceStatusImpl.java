// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.service;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import com.hedera.block.server.config.BlockNodeContext;
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

    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private WebServer webServer;

    private final int delayMillis;

    /**
     * Use the ServiceStatusImpl to check the status of the block node server and to shut it down if
     * necessary.
     *
     * @param blockNodeContext the block node context
     */
    @Inject
    public ServiceStatusImpl(@NonNull final BlockNodeContext blockNodeContext) {
        this.delayMillis =
                blockNodeContext.configuration().getConfigData(ServiceConfig.class).delayMillis();
    }

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
     * @param className the name of the class stopping the service
     */
    public void stopRunning(final String className) {
        LOGGER.log(DEBUG, String.format("%s set the status to stopped", className));
        isRunning.set(false);
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
     *
     * @param className the name of the class stopping the service
     */
    public void stopWebServer(@NonNull final String className) {

        LOGGER.log(DEBUG, String.format("%s is stopping the server", className));

        // Flag the service to stop
        // accepting new connections
        isRunning.set(false);

        try {
            // Delay briefly while outbound termination messages
            // are sent to the consumers and producers, etc.
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            LOGGER.log(ERROR, "An exception was thrown waiting to shut down the server: ", e);
        }

        // Stop the web server
        webServer.stop();
    }
}
