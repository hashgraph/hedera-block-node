// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.path;

import com.hedera.block.common.utils.FileUtilities;
import com.hedera.block.common.utils.Preconditions;
import com.hedera.block.server.Constants;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig;
import com.hedera.block.server.persistence.storage.PersistenceStorageConfig.CompressionType;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipFile;

/**
 * A Block path resolver for block-as-file.
 */
public final class BlockAsLocalFilePathResolver implements BlockPathResolver {
    private static final int MAX_LONG_DIGITS = 19;
    private final Path liveRootPath;
    private final Path archiveRootPath;
    private final int archiveDirDepth;
    private final int archiveGroupSize;
    private final DecimalFormat longLeadingZeroesFormat;
    private final DecimalFormat blockDirDepthFormat;

    /**
     * Constructor.
     *
     * @param config valid, {@code non-null} instance of
     * {@link PersistenceStorageConfig} used for initializing the resolver
     */
    private BlockAsLocalFilePathResolver(@NonNull final PersistenceStorageConfig config) {
        this.liveRootPath = Path.of(config.liveRootPath());
        this.archiveRootPath = Path.of(config.archiveRootPath());
        this.archiveGroupSize = config.archiveBatchSize();
        this.archiveDirDepth = MAX_LONG_DIGITS - (int) Math.log10(this.archiveGroupSize);
        this.longLeadingZeroesFormat = new DecimalFormat("0".repeat(MAX_LONG_DIGITS));
        this.blockDirDepthFormat = new DecimalFormat("0".repeat(archiveDirDepth));
    }

    /**
     * This method creates and returns a new instance of
     * {@link BlockAsLocalFilePathResolver}.
     *
     * @param config valid, {@code non-null} instance of
     * {@link PersistenceStorageConfig} used for initializing the resolver
     * @return a new, fully initialized instance of {@link BlockAsLocalFilePathResolver}
     */
    public static BlockAsLocalFilePathResolver of(@NonNull final PersistenceStorageConfig config) {
        return new BlockAsLocalFilePathResolver(config);
    }

    @NonNull
    @Override
    public Path resolveLiveRawPathToBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        final String rawBlockNumber = String.format("%0" + MAX_LONG_DIGITS + "d", blockNumber);
        final String[] blockPath = rawBlockNumber.split("");
        final String blockFileName = rawBlockNumber.concat(Constants.BLOCK_FILE_EXTENSION);
        blockPath[blockPath.length - 1] = blockFileName;
        return Path.of(liveRootPath.toString(), blockPath);
    }

    @Override
    public Optional<LiveBlockPath> findLiveBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        final Path rawLiveBlockPath = resolveLiveRawPathToBlock(blockNumber); // here is the raw path, no extension
        Optional<LiveBlockPath> result = Optional.empty();
        final CompressionType[] allCompressionTypes = CompressionType.values();
        for (int i = 0; i < allCompressionTypes.length; i++) {
            final CompressionType localCompressionType = allCompressionTypes[i];
            final Path compressionExtendedBlockPath =
                    FileUtilities.appendExtension(rawLiveBlockPath, localCompressionType.getFileExtension());
            if (Files.exists(compressionExtendedBlockPath)) {
                final Path dirPath = compressionExtendedBlockPath.getParent();
                final String blockFileName =
                        compressionExtendedBlockPath.getFileName().toString();
                final LiveBlockPath toReturn =
                        new LiveBlockPath(blockNumber, dirPath, blockFileName, localCompressionType);
                result = Optional.of(toReturn);
                break;
            }
        }
        return result;
    }

    @Override
    public Optional<ArchiveBlockPath> findArchivedBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        final ArchiveBlockPath rawArchiveBlockPath =
                resolveRawArchivePath(blockNumber); // here is the raw path, no extension
        final Path resolvedZipFilePath = rawArchiveBlockPath.dirPath().resolve(rawArchiveBlockPath.zipFileName());
        Optional<ArchiveBlockPath> result = Optional.empty();
        if (Files.exists(resolvedZipFilePath)) {
            try (final ZipFile zipFile = new ZipFile(resolvedZipFilePath.toFile())) {
                final String rawEntryName = rawArchiveBlockPath.zipEntryName();
                final CompressionType[] allCompressionTypes = CompressionType.values();
                for (int i = 0; i < allCompressionTypes.length; i++) {
                    final CompressionType localCompressionType = allCompressionTypes[i];
                    final String compressionExtendedEntry =
                            rawEntryName.concat(localCompressionType.getFileExtension());
                    if (Objects.nonNull(zipFile.getEntry(compressionExtendedEntry))) {
                        final ArchiveBlockPath toReturn = new ArchiveBlockPath(
                                rawArchiveBlockPath.blockNumber(),
                                rawArchiveBlockPath.dirPath(),
                                rawArchiveBlockPath.zipFileName(),
                                compressionExtendedEntry,
                                localCompressionType);
                        result = Optional.of(toReturn);
                        break;
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e); // todo should we propagate this exception?
            }
        }
        return result;
    }

    /**
     * This method resolves the path to where an archived block would reside. No
     * compression extension is appended to the file name.
     * @param blockNumber the block number to look for
     * @return an {@link ArchiveBlockPath} containing the raw path resolved
     */
    private ArchiveBlockPath resolveRawArchivePath(final long blockNumber) {
        final long dividedNumber = Math.floorDiv(blockNumber, archiveGroupSize);
        final String formattedNumber = blockDirDepthFormat.format(dividedNumber);
        final StringBuilder pathBuilder = new StringBuilder(archiveDirDepth * 2);
        final char[] arr = formattedNumber.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            pathBuilder.append(arr[i]).append("/");
        }
        pathBuilder.setCharAt(pathBuilder.length() - 1, '.');
        pathBuilder.append("zip");
        final String dst = pathBuilder.toString();
        // use the symlink from the live root path
        final Path destPath = liveRootPath.resolve(Path.of(dst));
        final String rawBlockFileName =
                longLeadingZeroesFormat.format(blockNumber).concat(Constants.BLOCK_FILE_EXTENSION);
        return new ArchiveBlockPath(
                blockNumber,
                destPath.getParent(),
                destPath.getFileName().toString(),
                rawBlockFileName,
                CompressionType.NONE);
    }
}
