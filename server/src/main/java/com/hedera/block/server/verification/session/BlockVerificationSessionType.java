/*
 * Copyright (C) 2024-2025 Hedera Hashgraph, LLC
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

package com.hedera.block.server.verification.session;

/**
 * Defines the types of block verification sessions.
 */
public enum BlockVerificationSessionType {
    /**
     * An asynchronous block verification session, where the verification is done in a separate thread.
     */
    ASYNC,
    /**
     * A synchronous block verification session, where the verification is done in the same thread.
     */
    SYNC
}
