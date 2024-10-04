package com.hedera.block.tools.commands;

import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.node.base.Transaction;
import com.hedera.hapi.node.transaction.SignedTransaction;
import com.hedera.hapi.node.transaction.TransactionBody;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@SuppressWarnings({"DataFlowIssue", "unused"})
@Command(name = "info", description = "Prints info for block files")
public class BlockInfo implements Runnable {

    @Parameters(index = "0..*")
    private File[] files;

    @Override
    public void run() {
        if (files == null || files.length == 0) {
            System.err.println("No files to convert");
        } else {
            for (File file : files) {
                convert(file);
            }
        }
    }

    private void convert(File blockProtoFile) {
        if (blockProtoFile.isDirectory()) {
            File[] files = blockProtoFile.listFiles();
            if (files != null) {
                Arrays.stream(files)
                        .sorted(Comparator.comparing(File::getName))
                        .forEach(this::convert);
            }
        } else if (blockProtoFile.exists()) {
            if (blockProtoFile.getName().endsWith(".blk") || blockProtoFile.getName().endsWith(".blk.gz")) {
                try (FileInputStream fIn = new FileInputStream(blockProtoFile)) {
                    byte[] uncompressedData;
                    if (blockProtoFile.getName().endsWith(".gz")) {
                        uncompressedData = new GZIPInputStream(fIn).readAllBytes();
                    } else {
                        uncompressedData = fIn.readAllBytes();
                    }
                    long start = System.currentTimeMillis();
                    final Block block = Block.PROTOBUF.parse(Bytes.wrap(uncompressedData));
                    long end = System.currentTimeMillis();
                    printInfo(block, end - start);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("File not supported : " + blockProtoFile.getName());
            }
        } else {
            System.out.println("File not found : "+blockProtoFile.getName());
        }
    }

    public static void printInfo(Block block, long parseTimeMs) {
            long numOfTransactions = block.items().stream().filter(BlockItem::hasEventTransaction).count();
            System.out.printf("Block [%,d] contains = %,d items, %,d transactions : parse time = %,d ms\n",
                    block.items().getFirst().blockHeader().number(),
                    block.items().size(),
                    numOfTransactions,
                    parseTimeMs);
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
            transactionTypeCounts.forEach((k,v) -> System.out.printf("    %s = %,d transactions\n", k, v));
    }
}
