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

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.INFO;

import java.io.IOException;

/** Main class for the block node server */
public class Server {

    private static final Logger LOGGER = System.getLogger(Server.class.getName());

    private Server() {}

    /**
     * Main entrypoint for the block node server
     *
     * @param args Command line arguments. Not used at present.
     * @throws IOException if there is an error starting the server
     */
    public static void main(final String[] args) throws IOException {
        LOGGER.log(INFO, "Starting BlockNode Server");
        final BlockNodeAppInjectionComponent daggerComponent =
                DaggerBlockNodeAppInjectionComponent.create();
        final BlockNodeApp blockNodeApp = daggerComponent.getBlockNodeApp();
        blockNodeApp.start();
    }
}
