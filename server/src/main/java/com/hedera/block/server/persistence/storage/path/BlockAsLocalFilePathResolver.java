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
    private final int archiveGroupSize;
    private final int archiveDirDepth;
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
        final String[] blockPath = getRawBlockPath(blockNumber);
        blockPath[blockPath.length - 1] = blockPath[blockPath.length - 1].concat(Constants.BLOCK_FILE_EXTENSION);
        return Path.of(liveRootPath.toString(), blockPath);
    }

    @NonNull
    @Override
    public Path resolveLiveRawUnverifiedPathToBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        final String[] blockPath = getRawBlockPath(blockNumber);
        blockPath[blockPath.length - 1] =
                blockPath[blockPath.length - 1].concat(Constants.UNVERIFIED_BLOCK_FILE_EXTENSION);
        return Path.of(liveRootPath.toString(), blockPath);
    }

    @NonNull
    @Override
    public Path resolveRawPathToArchiveParentUnderLive(final long blockNumber) {
        return resolveRawArchivingTarget(blockNumber, liveRootPath, "");
    }

    @NonNull
    @Override
    public Path resolveRawPathToArchiveParentUnderArchive(final long blockNumber) {
        return resolveRawArchivingTarget(blockNumber, archiveRootPath, ".zip");
    }

    @NonNull
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

    @NonNull
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
                                rawArchiveBlockPath.dirPath(),
                                rawArchiveBlockPath.zipFileName(),
                                compressionExtendedEntry,
                                localCompressionType,
                                rawArchiveBlockPath.blockNumber());
                        result = Optional.of(toReturn);
                        break;
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return result;
    }

    @Override
    public boolean existsVerifiedBlock(final long blockNumber) {
        Preconditions.requireWhole(blockNumber);
        return findLiveBlock(blockNumber).isPresent()
                || findArchivedBlock(blockNumber).isPresent();
    }

    @Override
    public void markVerified(final long blockNumber) throws IOException {
        Preconditions.requireWhole(blockNumber);
        final Path pathToUnverifiedBlockNoCompressionExtension = resolveLiveRawUnverifiedPathToBlock(blockNumber);
        final CompressionType[] allCompressionTypes = CompressionType.values();
        for (int i = 0; i < allCompressionTypes.length; i++) {
            final CompressionType compressionType = allCompressionTypes[i];
            final Path compressionExtendedUnverifiedPath = FileUtilities.appendExtension(
                    pathToUnverifiedBlockNoCompressionExtension, compressionType.getFileExtension());
            if (Files.exists(compressionExtendedUnverifiedPath)) {
                doMarkUnverified(compressionExtendedUnverifiedPath);
                break;
            }
        }
    }

    /**
     * This method resolves the path to where an archived block would reside. No
     * compression extension is appended to the file name.
     * @param blockNumber the block number to look for
     * @return an {@link ArchiveBlockPath} containing the raw path resolved
     */
    ArchiveBlockPath resolveRawArchivePath(final long blockNumber) {
        final Path zipRootUnderLiveLocation = resolveRawPathToArchiveParentUnderLive(blockNumber);
        final String zipEntryName = zipRootUnderLiveLocation
                .relativize(resolveLiveRawPathToBlock(blockNumber))
                .toString();
        final Path zipFileSymlink = FileUtilities.appendExtension(zipRootUnderLiveLocation, ".zip");
        return new ArchiveBlockPath(
                zipFileSymlink.getParent(),
                zipFileSymlink.getFileName().toString(),
                zipEntryName,
                CompressionType.NONE,
                blockNumber);
    }

    private String[] getRawBlockPath(final long blockNumber) {
        final String rawBlockNumber = longLeadingZeroesFormat.format(blockNumber);
        final String[] split = rawBlockNumber.split("");
        split[split.length - 1] = rawBlockNumber;
        return split;
    }

    private void doMarkUnverified(final Path targetToMove) throws IOException {
        final String verifiedBlockFileName = targetToMove
                .getFileName()
                .toString()
                .replace(Constants.UNVERIFIED_BLOCK_FILE_EXTENSION, Constants.BLOCK_FILE_EXTENSION);
        Files.move(targetToMove, targetToMove.resolveSibling(verifiedBlockFileName));
    }

    private Path resolveRawArchivingTarget(final long blockNumber, final Path basePath, final String extension) {
        final long batchStartNumber = (blockNumber / archiveGroupSize) * archiveGroupSize;
        final String formattedBatchStartNumber = longLeadingZeroesFormat.format(batchStartNumber);
        final String[] blockPath = formattedBatchStartNumber.split("");
        final int targetFilePosition = archiveDirDepth - 1;
        final String targetFileName = blockPath[targetFilePosition] + extension;
        blockPath[targetFilePosition] = targetFileName;
        // Construct the path
        Path result = basePath;
        for (int i = 0; i < targetFilePosition; i++) {
            result = result.resolve(blockPath[i]);
        }
        result = result.resolve(targetFileName);
        return result;
    }
}
