// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.archive;

import com.hedera.block.common.utils.FileUtilities;
import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.path.BlockPathResolver;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * TODO: add documentation
 */
public final class AsyncBlockAsLocalFileArchiver implements AsyncLocalBlockArchiver {
    private static final System.Logger LOGGER = System.getLogger(AsyncBlockAsLocalFileArchiver.class.getName());
    private final BlockPathResolver pathResolver;
    private final long blockNumberThreshold;

    AsyncBlockAsLocalFileArchiver(
            final long blockNumberThreshold,
            @NonNull final PersistenceStorageConfig config,
            @NonNull final BlockPathResolver pathResolver) {
        this.blockNumberThreshold = blockNumberThreshold;
        this.pathResolver = Objects.requireNonNull(pathResolver);
        Preconditions.requireWhole(blockNumberThreshold);
        final int archiveGroupSize = config.archiveBatchSize();
        if (blockNumberThreshold % archiveGroupSize != 0) { // @todo(517) require divisible exactly by
            throw new IllegalArgumentException("Block number must be divisible by " + archiveGroupSize);
        }
    }

    @Override
    public void run() {
        try {
            doArchive();
        } catch (final IOException e) {
            // todo return a result instead of exception
            throw new RuntimeException(e);
        }
    }

    private void doArchive() throws IOException {
        final long upperBound = blockNumberThreshold - 1;
        final Path rootToArchive = pathResolver.resolveRawPathToArchiveParentUnderLive(upperBound);
        LOGGER.log(Level.DEBUG, "Archiving Block Files under [%s]".formatted(rootToArchive));
        final List<Path> pathsToArchive;
        try (final Stream<Path> tree = Files.walk(rootToArchive)) {
            pathsToArchive = tree.sorted(Comparator.reverseOrder())
                    .filter(Files::isRegularFile)
                    .toList();
        }
        if (!pathsToArchive.isEmpty()) {
            final Path zipFilePath = archiveInZip(upperBound, pathsToArchive, rootToArchive);
            createSymlink(rootToArchive, zipFilePath);
            deleteLive(rootToArchive);
        }
    }

    private Path archiveInZip(final long upperBound, final List<Path> pathsToArchive, final Path rootToArchive)
            throws IOException {
        final Path zipFilePath = pathResolver.resolveRawPathToArchiveParentUnderArchive(upperBound);
        if (!Files.exists(zipFilePath)) {
            // @todo(517) should we assume something if the zip file already exists? If yes, what and how to
            // handle?
            FileUtilities.createFile(zipFilePath);
        }
        LOGGER.log(Level.DEBUG, "Target Zip Path [%s]".formatted(zipFilePath));
        try (final ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            for (int i = 0; i < pathsToArchive.size(); i++) {
                final Path pathToArchive = pathsToArchive.get(i);
                final String relativizedEntryName =
                        rootToArchive.relativize(pathToArchive).toString();
                final ZipEntry zipEntry = new ZipEntry(relativizedEntryName);
                LOGGER.log(Level.TRACE, "Adding Zip Entry [%s] to zip file [%s]".formatted(zipEntry, zipFilePath));
                out.putNextEntry(zipEntry);
                Files.copy(pathToArchive, out);
                out.closeEntry();
                LOGGER.log(
                        Level.TRACE,
                        "Zip Entry [%s] successfully added to zip file [%s]".formatted(zipEntry, zipFilePath));
            }
        }
        LOGGER.log(Level.DEBUG, "Zip File [%s] successfully created".formatted(zipFilePath));
        return zipFilePath;
    }

    private static void createSymlink(final Path rootToArchive, final Path zipFilePath) throws IOException {
        // we need to create a symlink to the zip file we just created so readers can find it
        final Path liveSymlink = FileUtilities.appendExtension(rootToArchive, ".zip");
        Files.createSymbolicLink(liveSymlink, zipFilePath);
        LOGGER.log(Level.DEBUG, "Symlink [%s <-> %s] created".formatted(liveSymlink, zipFilePath));
    }

    private void deleteLive(final Path rootToArchive) throws IOException {
        // we need to move the live dir that we just archived so readers will no longer be able
        // to find it, hence they will fall back to search for the symlink we just made as well
        // in the meantime, while readers get data from the symlink, we can safely delete the
        // live dir
        final Path movedToDelete = FileUtilities.appendExtension(rootToArchive, "del");
        Files.move(rootToArchive, movedToDelete);
        try (Stream<Path> paths = Files.walk(movedToDelete)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
