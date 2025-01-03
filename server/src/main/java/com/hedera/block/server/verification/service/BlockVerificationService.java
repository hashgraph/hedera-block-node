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

package com.hedera.block.server.verification.service;

import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.pbj.runtime.ParseException;
import java.util.List;

/**
 * Service that handles the verification of block items, it receives items from the handler.
 */
public interface BlockVerificationService {
    /**
     * Everytime the handler receives a block item, it will call this method to verify the block item.
     *
     * @param blockItems the block items to add to the verification service
     * @throws ParseException if the block items are invalid
     */
    void onBlockItemsReceived(List<BlockItemUnparsed> blockItems) throws ParseException;
}
