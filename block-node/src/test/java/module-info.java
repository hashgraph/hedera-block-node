module block.node.test {
    // works
    requires com.hedera.node.hapi;
    requires com.swirlds.common;
    requires com.swirlds.platform.core;
    requires org.bouncycastle.provider;

    // fails
//    requires transitive com.swirlds.platform;
//    requires com.swirlds.platform.crypto;
//    requires transitive com.hedera.pbj.runtime;


//    requires com.hedera.node.hapi.node.base;

//    requires com.google.protobuf;
//    requires com.google.common;
//    requires transitive com.hedera.hapi;
//    requires transitive com.hedera.hapi;

//    requires transitive com.hedera.hapi.node.base;
//    requires transitive com.hedera.hapi.node.transaction;
//    requires transitive com.hedera.node.app;
//    requires transitive com.swirlds.common;
//    requires transitive com.swirlds.platform;
//    requires transitive com.hedera.pbj.runtime;
}