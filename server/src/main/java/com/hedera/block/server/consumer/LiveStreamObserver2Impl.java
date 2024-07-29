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

package com.hedera.block.server.consumer;

import com.hedera.block.protos.BlockStreamServiceGrpcProto;

public class LiveStreamObserver2Impl
        implements LiveStreamObserver<
                BlockStreamServiceGrpcProto.Block, BlockStreamServiceGrpcProto.BlockResponse> {

    @Override
    public void notify(BlockStreamServiceGrpcProto.Block block) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onNext(BlockStreamServiceGrpcProto.BlockResponse value) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onError(Throwable t) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onCompleted() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
