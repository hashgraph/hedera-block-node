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

package com.hedera.block.server.mediator;

import com.hedera.block.server.notifier.Notifiable;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.SubscribeStreamResponseUnparsed;
import java.util.List;

/**
 * Use this interface to combine the contract for mediating the live stream of blocks from the
 * Hedera network with the contract to be notified of critical system events.
 */
public interface LiveStreamMediator
        extends StreamMediator<List<BlockItemUnparsed>, SubscribeStreamResponseUnparsed>, Notifiable {}
