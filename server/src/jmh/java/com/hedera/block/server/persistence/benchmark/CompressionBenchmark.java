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

package com.hedera.block.server.persistence.benchmark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class CompressionBenchmark {

    @Param({"xsmall", "small", "medium", "large", "xlarge"})
    public String fileSize;

    @Param({"1", "2", "3", "4", "5"})
    public String fileIndex;

    @Param({"zip", "gzip"})
    public String compressionAlgorithm;

    private byte[] dataToCompress;

    @Setup(Level.Trial)
    public void setUp() throws IOException {
        String fileName = "src/jmh/resources/data/" + fileSize + "/000" + fileIndex + ".blk";
        dataToCompress = Files.readAllBytes(Paths.get(fileName));
    }

    @Benchmark
    @Fork(1)
    @Warmup(iterations = 2)
    @Threads(1)
    public void compress(CompressionCounters counters, Blackhole blackhole) throws IOException {
        byte[] compressedData;
        switch (compressionAlgorithm) {
            case "zip":
                compressedData = compressZip(dataToCompress);
                break;
            case "gzip":
                compressedData = compressGzip(dataToCompress);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown compression algorithm: " + compressionAlgorithm);
        }
        // Update counters
        counters.originalSize = dataToCompress.length;
        counters.compressedSize = compressedData.length;

        // Consume the compressed data
        blackhole.consume(compressedData);
    }

    private byte[] compressZip(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("compressed");
            zipOut.putNextEntry(entry);
            zipOut.write(data);
            zipOut.closeEntry();
        }
        return baos.toByteArray();
    }

    private byte[] compressGzip(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
        }
        return baos.toByteArray();
    }

    @State(Scope.Thread)
    @AuxCounters(AuxCounters.Type.EVENTS)
    @BenchmarkMode(Mode.SingleShotTime)
    public static class CompressionCounters {
        public long originalSize = 0;
        public long compressedSize = 0;

        @Setup(Level.Iteration)
        public void reset() {
            originalSize = 0;
            compressedSize = 0;
        }

        /** Returns the compression ratio. */
        public double compressionRatio() {
            if (compressedSize == 0) {
                return 0;
            }
            return (double) originalSize / compressedSize;
        }

        /** Returns the compression ratio. */
        public double invertedCompressionRatio() {
            if (originalSize == 0) {
                return 0;
            }
            return (double) compressedSize / originalSize;
        }

        @TearDown(Level.Iteration)
        public void printCompressionRatio() {
            System.out.println("Original Size: " + originalSize);
            System.out.println("Compressed Size: " + compressedSize);
            System.out.println("Compression Ratio: " + compressionRatio());
        }
    }
}
