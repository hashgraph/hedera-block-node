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

package com.hedera.block.server.persistence.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PersistenceStorageConfigTest {

    @Test
    void testPersistenceStorageConfig() {
        PersistenceStorageConfig persistenceStorageConfig =
                new PersistenceStorageConfig("rootPath");
        assertEquals("rootPath", persistenceStorageConfig.rootPath());
    }

    @Test
    void testPersistenceStorageConfig_emptyRootPath() throws IOException {
        final String expectedDefaultRootPath =
                Paths.get("").toAbsolutePath().resolve("data").toString();
        // delete if exists
        deleteDirectory(Paths.get(expectedDefaultRootPath));

        PersistenceStorageConfig persistenceStorageConfig = new PersistenceStorageConfig("");
        assertEquals(expectedDefaultRootPath, persistenceStorageConfig.rootPath());
    }

    public static void deleteDirectory(Path path) throws IOException {
        if(!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(
                            p -> {
                                try {
                                    Files.delete(p);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
        }
    }
}
