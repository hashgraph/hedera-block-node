package com.hedera.block.tools.commands.record2blocks.mirrornode;


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
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Download mirror node record table CSV dump from GCP bucket
 */
@SuppressWarnings("CallToPrintStackTrace")
@Command(name = "fetchRecordsCsv", description = "Download mirror node record table CSV dump from GCP bucket")
public class FetchMirrorNodeRecordsCsv implements Runnable {
    /** The GCP bucket name that contains CSV dumps of mirror node */
    private static final String bucketName = "mirrornode-db-export";

    /** The path to the record table CSV in bucket */
    private static final String objectPath = "0.113.2/record_file.csv.gz";

    /** The path to the record table CSV from mirror node, gzipped. */
    @Option(names = {"--record-csv"},
            description = "Path to the record table CSV from mirror node, gzipped.")
    private Path recordsCsvFile = Path.of("data/record_file.csv.gz");

    /**
     * Download the record table CSV from mirror node GCP bucket
     */
    @Override
    public void run() {
        try {
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

            bucket.list(Storage.BlobListOption.prefix("0.113.2"), Storage.BlobListOption.userProject(projectId))
                    .streamValues().map(Blob::getName).forEach(System.out::println);

            // Read the object from the bucket with requester pays option
            Blob blob = bucket.get(objectPath, Storage.BlobGetOption.userProject(projectId));
            // download file
            try (ProgressOutputStream out = new ProgressOutputStream(new FileOutputStream(recordsCsvFile.toFile()),
                    blob.getSize(), recordsCsvFile.getFileName().toString())) {
                blob.downloadTo(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * A simple output stream that prints progress to the console.
     */
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
