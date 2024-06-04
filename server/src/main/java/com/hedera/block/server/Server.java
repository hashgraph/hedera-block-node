package com.hedera.block.server;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;

/**
 * Main class for the block node server
 */
public class Server {
    private Server() {
        // Not meant to be instantiated
    }

    /**
     * Main entrypoint for the block node server
     *
     * @param args Command line arguments. Not used at present,
     */
    public static void main(String[] args) {
        WebServer.builder()
                .port(8080)
                .addRouting(HttpRouting.builder()
                        .get("/greet", (req, res) -> res.send("Hello World!")))
                .build()
                .start();
    }
}
