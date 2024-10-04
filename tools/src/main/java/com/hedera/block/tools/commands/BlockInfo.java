package com.hedera.block.tools.commands;

import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.node.base.Transaction;
import com.hedera.hapi.node.transaction.SignedTransaction;
import com.hedera.hapi.node.transaction.TransactionBody;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@SuppressWarnings({"DataFlowIssue", "unused", "StringConcatenationInsideStringBufferAppend", "DuplicatedCode"})
@Command(name = "info", description = "Prints info for block files")
public class BlockInfo implements Runnable {

    @Parameters(index = "0..*")
    private File[] files;

    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    @Option(names = {"-ms", "--min-size"}, description = "minimum file size in megabytes")
    private double minSizeMb = Double.MAX_VALUE;

    @Override
    public void run() {
        if (files == null || files.length == 0) {
            System.err.println("No files to convert");
        } else {
            Arrays.stream(files)
                    .map(File::toPath)
                    .flatMap(path -> {
                        try {
                            return Files.walk(path);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".blk") || file.getFileName().toString()
                            .endsWith(".blk.gz"))
                    .filter(file -> { // handle min file size
                        try {
                            return minSizeMb == Double.MAX_VALUE || Files.size(file) / 1024.0 / 1024.0 >= minSizeMb;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .sorted(Comparator.comparing(file -> file.getFileName().toString()))
                    .parallel()
                    .map(this::printInfo)
                    .forEachOrdered(System.out::println);
        }
    }


    public String printInfo(Path blockProtoFile) {
        try (InputStream fIn = Files.newInputStream(blockProtoFile)) {
            byte[] uncompressedData;
            if (blockProtoFile.getFileName().toString().endsWith(".gz")) {
                uncompressedData = new GZIPInputStream(fIn).readAllBytes();
            } else {
                uncompressedData = fIn.readAllBytes();
            }
            long start = System.currentTimeMillis();
            final Block block = Block.PROTOBUF.parse(Bytes.wrap(uncompressedData));
            long end = System.currentTimeMillis();
            return printInfo(block,
                    end - start,
                    Files.size(blockProtoFile),
                    uncompressedData.length);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            sw.append("Error processing file : "+blockProtoFile+"\n");
            e.printStackTrace(new java.io.PrintWriter(sw));
            return sw.toString();
        }
    }

    public static String printInfo(Block block, long parseTimeMs, long originalFileSizeBytes, long uncompressedFileSizeBytes) {
        final StringBuffer output = new StringBuffer();
        long numOfTransactions = block.items().stream().filter(BlockItem::hasEventTransaction).count();
        String json = ConvertToJson.toJson(block, false);
        // count number of '{' chars in json string to get number of objects
        final long numberOfObjectsInBlock = json.chars().filter(c -> c == '{').count();
        output.append(String.format("Block [%d] contains = %d items, %d transactions, %d java objects : parse time = %d ms\n",
                block.items().getFirst().blockHeader().number(),
                block.items().size(),
                numOfTransactions,
                numberOfObjectsInBlock,
                parseTimeMs));

        final double originalFileSizeMb = originalFileSizeBytes / 1024.0 / 1024.0;
        final double uncompressedFileSizeMb = uncompressedFileSizeBytes / 1024.0 / 1024.0;
        final double compressionPercent = 100.0 - (originalFileSizeMb / uncompressedFileSizeMb * 100.0);
        output.append(String.format("    Original File Size = %,.2f MB, Uncompressed File Size = %,.2f MB, Compression = %.2f%%\n",
                originalFileSizeMb, uncompressedFileSizeMb, compressionPercent));
        Map<String,Long> transactionTypeCounts = new HashMap<>();
        long numOfSystemTransactions = block.items().stream()
                .filter(BlockItem::hasEventTransaction)
                .filter(item -> item.eventTransaction().hasStateSignatureTransaction())
                .count();
        if (numOfSystemTransactions > 0) {
            transactionTypeCounts.put("SystemSignature",numOfTransactions);
        }
        block.items().stream()
                .filter(BlockItem::hasEventTransaction)
                .filter(item -> item.eventTransaction().hasApplicationTransaction())
                .map(item -> {
                    try {
                        return TransactionBody.PROTOBUF.parse(
                                        SignedTransaction.PROTOBUF.parse(
                                                        Transaction.PROTOBUF.parse(item.eventTransaction().applicationTransaction()).signedTransactionBytes())
                                                .bodyBytes())
                                .data().kind().toString();
                    } catch (ParseException e) {
                        System.err.println("Error parsing transaction body : "+e.getMessage());
                        throw new RuntimeException(e);
                    }
                })
                .forEach(kind -> transactionTypeCounts.put(kind, transactionTypeCounts.getOrDefault(kind, 0L)+1));
        transactionTypeCounts.forEach((k,v) -> output.append(String.format("    %s = %,d transactions\n", k, v)));
        return output.toString();
    }

    @SuppressWarnings("rawtypes")
    public static long countObjectsInTree(Object object) {
        if (object == null) {
            return 0;
        }
        long count = 1;
        final Class objectCass = object.getClass();
        // using reflection walk the object tree and count the number of objects
        // check if the object is an array
        if (objectCass.isArray()) {
            // check if array is primitive
            if (!objectCass.getComponentType().isPrimitive()) {
                for (Object element : (Object[]) object) {
                    count += countObjectsInTree(element);
                }
            }
            return count;
        }
        // check if the object is a collection
        if (object instanceof Collection) {
            for (Object element : (Collection) object) {
                count += countObjectsInTree(element);
            }
            return count;
        }
        // check if object is java record
        if (objectCass.isRecord()) {
            for (Method method :object.getClass().getMethods()) {
                if (Character.isLowerCase(method.getName().charAt(0)) &&
                        method.getReturnType() != void.class &&
                        method.getParameterCount() == 0) {
                    try {
                        Object value = method.invoke(object);
                        if (value != null) {
                            count += countObjectsInTree(value);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return count;
        }
        System.err.println("Unhandled object type : "+objectCass.getName());
        return count;
    }
}
