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

import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/** Defines the contract for health http service, needed for implementing an standard */
public interface HealthService extends HttpService {
    /** The path for the health group endpoints. Root path for all health endpoints.
     *
     * @return the root path for the health group endpoints
     *  */
    String getHealthRootPath();

    /**
     * Configures the health routes for the server.
     *
     * @param httpRules is used to configure the health endpoints routes
     */
    @Override
    void routing(HttpRules httpRules);

    void handleLiveness(ServerRequest req, ServerResponse res);

    void handleReadiness(ServerRequest req, ServerResponse res);
}