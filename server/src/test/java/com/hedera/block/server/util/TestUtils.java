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

package com.hedera.block.server.util;

import java.io.File;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public final class TestUtils {
    private TestUtils() {}

    private static final String NO_PERMS = "---------";
    private static final String NO_READ = "-wx-wx-wx";
    private static final String NO_WRITE = "r-xr-xr-x";

    public static boolean deleteDirectory(File directoryToBeDeleted) {

        if (!directoryToBeDeleted.exists()) {
            return true;
        }

        if (directoryToBeDeleted.isDirectory()) {
            File[] allContents = directoryToBeDeleted.listFiles();
            if (allContents != null) {
                for (File file : allContents) {
                    deleteDirectory(file);
                }
            }
        }

        return directoryToBeDeleted.delete();
    }

    public static FileAttribute<Set<PosixFilePermission>> getNoPerms() {
        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(NO_PERMS));
    }

    public static FileAttribute<Set<PosixFilePermission>> getNoRead() {
        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(NO_READ));
    }

    public static FileAttribute<Set<PosixFilePermission>> getNoWrite() {
        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(NO_WRITE));
    }
}
