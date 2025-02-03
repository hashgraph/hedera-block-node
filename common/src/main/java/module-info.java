// SPDX-License-Identifier: Apache-2.0
module com.hedera.block.common {
    exports com.hedera.block.common.constants;
    exports com.hedera.block.common.utils;
    exports com.hedera.block.common.hasher;

    requires transitive com.hedera.block.stream;
    requires transitive com.hedera.pbj.runtime;
    requires com.swirlds.common;
    requires static com.github.spotbugs.annotations;
}
