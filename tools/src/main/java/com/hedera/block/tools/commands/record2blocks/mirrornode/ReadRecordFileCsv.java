package com.hedera.block.tools.commands.record2blocks.mirrornode;

import static com.hedera.block.tools.commands.record2blocks.Main.DATA_DIR;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

/**
 * Read the record_file.csv.gz file from mirror node and store the data in a SQLite database.
 *
 * <pre>
 * columns
 * 0    name,                   // record stream file name
 * 1    load_start,
 * 2    load_end,
 * 3    hash,
 * 4    prev_hash,
 * 5    consensus_start,
 * 6    consensus_end,
 * 7    count,
 * 8    digest_algorithm,
 * 9    hapi_version_major,
 * 10   hapi_version_minor,
 * 11   hapi_version_patch,
 * 12   version,
 * 13   file_hash,
 * 14   bytes,
 * 15   index,
 * 16   gas_used,
 * 17   logs_bloom,
 * 18   size,
 * 19   sidecar_count,
 * 20   node_id
 * </pre>
 */
public class ReadRecordFileCsv {
    public static final AtomicLong totalLines = new AtomicLong(0);

    public static void main(String[] args) {
        Path recordFile = DATA_DIR.resolve("record_file.csv.gz");

        try(var reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(recordFile.toFile()))));
                Connection connection = DriverManager.getConnection("jdbc:sqlite:data/recordFiles.db");
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS blocks");
            String createTableQuery = """
                CREATE TABLE IF NOT EXISTS blocks (
                    blockNumber INTEGER PRIMARY KEY,
                    recordStreamFileName TEXT NOT NULL,
                    prevHash BLOB,
                    hapiVersionMajor INTEGER,
                    hapiVersionMinor INTEGER,
                    hapiVersionPatch INTEGER,
                    size INTEGER,
                    sidecarCount INTEGER,
                    node0 INTEGER DEFAULT 0,
                    node1 INTEGER DEFAULT 0,
                    node2 INTEGER DEFAULT 0,
                    node3 INTEGER DEFAULT 0,
                    node4 INTEGER DEFAULT 0,
                    node5 INTEGER DEFAULT 0,
                    node6 INTEGER DEFAULT 0,
                    node7 INTEGER DEFAULT 0,
                    node8 INTEGER DEFAULT 0,
                    node9 INTEGER DEFAULT 0,
                    node10 INTEGER DEFAULT 0,
                    node11 INTEGER DEFAULT 0,
                    node12 INTEGER DEFAULT 0,
                    node13 INTEGER DEFAULT 0,
                    node14 INTEGER DEFAULT 0,
                    node15 INTEGER DEFAULT 0,
                    node16 INTEGER DEFAULT 0,
                    node17 INTEGER DEFAULT 0,
                    node18 INTEGER DEFAULT 0,
                    node19 INTEGER DEFAULT 0,
                    node20 INTEGER DEFAULT 0,
                    node21 INTEGER DEFAULT 0,
                    node22 INTEGER DEFAULT 0,
                    node23 INTEGER DEFAULT 0,
                    node24 INTEGER DEFAULT 0,
                    node25 INTEGER DEFAULT 0,
                    node26 INTEGER DEFAULT 0,
                    node27 INTEGER DEFAULT 0,
                    node28 INTEGER DEFAULT 0,
                    node29 INTEGER DEFAULT 0,
                    node30 INTEGER DEFAULT 0,
                    node31 INTEGER DEFAULT 0,
                    node32 INTEGER DEFAULT 0,
                    node33 INTEGER DEFAULT 0,
                    node34 INTEGER DEFAULT 0
                )
                """;
            statement.execute(createTableQuery);
            // skip header
            reader.readLine();
            // read all lines
            reader.lines().forEach(csvLine -> {
                final long count = totalLines.incrementAndGet();
                if (count % 100_000 == 0) {
                    System.out.printf("\rtotalLines = %,d", count);
                }
                String[] parts = csvLine.split(",");
                try {
                    final long blockNumber = !parts[15].isEmpty() ? Long.parseLong(parts[15]) : -1;
                    final String recordStreamFileName = parts[0];
                    final String prevHash = parts[4];
                    final int hapiVersionMajor = !parts[9].isEmpty() ? Integer.parseInt(parts[9]) : -1;
                    final int hapiVersionMinor = !parts[10].isEmpty() ? Integer.parseInt(parts[10]) : -1;
                    final int hapiVersionPatch = !parts[11].isEmpty() ? Integer.parseInt(parts[11]) : -1;
                    final int size = !parts[18].isEmpty() ? Integer.parseInt(parts[18]) : -1;
                    final int sidecarCount = !parts[19].isEmpty() ? Integer.parseInt(parts[19]) : -1;
                    final int nodeId = !parts[20].isEmpty() ? Integer.parseInt(parts[20]) : -1;
                    if (blockNumber == -1 || nodeId == -1) {
                        System.err.println("Error blockNumber="+blockNumber+", nodeId="+nodeId+" on csvLine = " + csvLine);
                    } else {
                        statement.execute("""
                                INSERT INTO blocks (
                                    blockNumber,
                                    recordStreamFileName,
                                    prevHash,
                                    hapiVersionMajor,
                                    hapiVersionMinor,
                                    hapiVersionPatch,
                                    size,
                                    sidecarCount,
                                    node{NODE_ID}
                                ) VALUES (
                                    {BLOCK_NUMBER},
                                    '{RECORD_STREAM_FILE_NAME}',
                                    '{PREV_HASH}',
                                    {HAPI_VERSION_MAJOR},
                                    {HAPI_VERSION_MINOR},
                                    {HAPI_VERSION_PATCH},
                                    {SIZE},
                                    {SIDECAR_COUNT},
                                    1
                                )
                                ON CONFLICT(blockNumber) DO UPDATE SET node{NODE_ID}=1
                                """
                                .replace("{BLOCK_NUMBER}", String.valueOf(blockNumber))
                                .replace("{RECORD_STREAM_FILE_NAME}", recordStreamFileName)
                                .replace("{PREV_HASH}", prevHash)
                                .replace("{HAPI_VERSION_MAJOR}", String.valueOf(hapiVersionMajor))
                                .replace("{HAPI_VERSION_MINOR}", String.valueOf(hapiVersionMinor))
                                .replace("{HAPI_VERSION_PATCH}", String.valueOf(hapiVersionPatch))
                                .replace("{SIZE}", String.valueOf(size))
                                .replace("{SIDECAR_COUNT}", String.valueOf(sidecarCount))
                                .replace("{NODE_ID}", String.valueOf(nodeId))
                        );
                    }
                } catch(NumberFormatException | SQLException e) {
                    System.err.println("Error on csvLine = " + csvLine);
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
