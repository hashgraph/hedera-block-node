// SPDX-License-Identifier: Apache-2.0
module com.hedera.block.common {
    exports com.hedera.block.common.constants;
    exports com.hedera.block.common.utils;
    exports com.hedera.block.common.hasher;

    requires com.hedera.block.stream;
    requires com.hedera.pbj.runtime;
    requires com.swirlds.common;
    requires static com.github.spotbugs.annotations;
}
