package com.hedera.block.tools.commands.record2blocks.model;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage.BlobListOption;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NodeRecords {
    public static final String PATH_PREFIX = "recordstreams/record0.0.";
    public static final String RECORD_FILES_DIR = "record-files";
    private final int nodeAccountNumber;
    private final Bucket bucket;
    private final String nodeRecordsBucketPath;
    private final Path nodeLocalDir;
    private final Path recordFilesDir;
    /** Simple text file containing the next page token */
    private final Path nextPageFile;
    /** Cached listing files of all paths in bucket. Avoid extra list costs */
    private final Path listingFile;
    private String nextPageToken;

    public NodeRecords(int nodeAccountNumber, Bucket bucket, Path dataDir) {
        this.nodeAccountNumber = nodeAccountNumber;
        this.bucket = bucket;
        nodeRecordsBucketPath = PATH_PREFIX + nodeAccountNumber;
        nodeLocalDir = dataDir.resolve("node-" + nodeAccountNumber);
        recordFilesDir = nodeLocalDir.resolve(RECORD_FILES_DIR);
        nextPageFile = nodeLocalDir.resolve("next-page-id.txt");
        listingFile = nodeLocalDir.resolve("listing.csv");
        // create directories
        recordFilesDir.toFile().mkdirs();
        // read next page id if file exists
        if (Files.exists(nextPageFile)) {
            try {
                nextPageToken = Files.readString(nextPageFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // read listing file if exists
        if (Files.exists(listingFile)) {
            try {
                try (var linesStream = Files.lines(listingFile)) {
                    linesStream.forEach(line -> {
                        final String[] parts = line.split(",");
                        final String filePath = parts[0];
                        final long fileSize = Long.parseLong(parts[1]);
                        final String fileMD5 = parts[2];
                        handleNewBucketFile(filePath, fileSize, fileMD5);
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Read the next page of the bucket listing from cloud then save to CSV listing cache and process the listing
     * entries.
     */
    public void readNextPage() {
        final Page<Blob> page = nextPageToken == null ?
                bucket.list(BlobListOption.prefix(nodeRecordsBucketPath)):
                bucket.list(BlobListOption.prefix(nodeRecordsBucketPath), BlobListOption.pageToken(nextPageToken));
        // update nextPageToken, local and in file
        nextPageToken = page.getNextPageToken();
        try {
            Files.writeString(nextPageFile, nextPageToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // list page and save to listing file and process
        try (FileWriter listingFileWrite = new FileWriter(listingFile.toFile(),true)) {
            for (Blob blob : page.getValues()) {
                String filePath = blob.getName();
                long fileSize = blob.getSize();
                String fileMD5 = blob.getMd5();
                handleNewBucketFile(filePath, fileSize, fileMD5);
                listingFileWrite.write(filePath);
                listingFileWrite.write(',');
                listingFileWrite.write(String.valueOf(fileSize));
                listingFileWrite.write(',');
                listingFileWrite.write(fileMD5);
                listingFileWrite.write('\n');
            }
            listingFileWrite.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleNewBucketFile(String filePath, long fileSize, String fileMD5) {
        System.out.println("filePath = " + filePath+", fileSize = " + fileSize+", fileMD5 = " + fileMD5);
    }
}
