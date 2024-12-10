package com.hedera.block.tools.commands.record2blocks.mirrornode;

import static com.hedera.block.tools.commands.record2blocks.Main.DATA_DIR;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public class FetchMirrorNodeBlocksInfo {
    private static final String bucketName = "mirrornode-db-export";
    private static final String objectPath = "0.113.2/record_file.csv.gz";
    private static final Path localFilePath = DATA_DIR.resolve("record_file.csv.gz");

    public static void main(String[] args) throws Exception {
        // download file from GCP requester pays bucket


        // Load the current credentials
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

        // Get the project ID from the credentials
        String projectId = ServiceOptions.getDefaultProjectId();

        if (projectId != null) {
            System.out.println("Project ID: " + projectId);
        } else {
            System.out.println("Project ID not found.");
        }

        // Instantiates a GCP Storage client
        final Storage storage = StorageOptions.getDefaultInstance().getService();
        // Get the bucket
        final Bucket bucket = storage.get(bucketName, Storage.BucketGetOption.userProject(projectId));

        bucket.list(Storage.BlobListOption.prefix("0.113.2") ,Storage.BlobListOption.userProject(projectId))
                .streamValues().map(Blob::getName).forEach(System.out::println);

        // Read the object from the bucket with requester pays option
        Blob blob = bucket.get(objectPath, Storage.BlobGetOption.userProject(projectId));
        // download file
        try(ProgressOutputStream out = new ProgressOutputStream(new FileOutputStream(localFilePath.toFile()),
                blob.getSize(), localFilePath.getFileName().toString())) {
            blob.downloadTo(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ProgressOutputStream extends FilterOutputStream {
        private static final long MB = 1024 * 1024;
        private long bytesWritten = 0;
        private long size;
        private final String name;

        public ProgressOutputStream(OutputStream out, long size, String name) {
            super(out);
            this.size = size;
            this.name = name;
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            bytesWritten++;
            printProgress();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            bytesWritten += len;
            printProgress();
        }

        private void printProgress() {
            if (bytesWritten % MB == 0) {
                System.out.printf("\rProgress: %.0f%% - %,d MB written of %s",
                        bytesWritten/(double)size,
                        bytesWritten / MB,
                        name);
            }
        }
    }
}
