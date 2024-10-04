package com.hedera.block.tools.commands;

import com.hedera.hapi.block.stream.Block;
import com.hedera.hapi.block.stream.BlockItem;
import com.hedera.hapi.node.base.Transaction;
import com.hedera.hapi.node.transaction.SignedTransaction;
import com.hedera.hapi.node.transaction.TransactionBody;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.hedera.pbj.runtime.io.stream.WritableStreamingData;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@SuppressWarnings({"DataFlowIssue", "unused"})
@Command(name = "json", description = "Converts a binary block stream to JSON")
public class ConvertToJson implements Runnable {

    @Parameters(index = "0..*")
    private File[] files;

    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    @Option(names = {"-t", "--transactions"}, description = "expand transactions")
    private boolean expandTransactions = false;

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
                final File outputFile = new File(blockProtoFile.getAbsoluteFile().getParent(),
                        blockProtoFile.getName() + ".json");
                try (FileInputStream fIn = new FileInputStream(blockProtoFile)) {
                    byte[] uncompressedData;
                    if (blockProtoFile.getName().endsWith(".gz")) {
                        uncompressedData = new GZIPInputStream(fIn).readAllBytes();
                    } else {
                        uncompressedData = fIn.readAllBytes();
                    }
                    final Block block = Block.PROTOBUF.parse(Bytes.wrap(uncompressedData));
                    writeJsonBlock(block, outputFile);
                    final long numOfTransactions = block.items().stream().filter(BlockItem::hasEventTransaction)
                            .count();
                    final String blockNumber = block.items().size() > 1 && block.items().getFirst().hasBlockHeader() ?
                            String.valueOf(Objects.requireNonNull(block.items().getFirst().blockHeader()).number())
                            : "unknown";
                    System.out.println("Converted \"" + blockProtoFile.getName() + "\" "
                            + "Block [" + blockNumber + "] "
                            + "contains = " + block.items().size() + " items, " + numOfTransactions + " transactions");
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

    private void writeJsonBlock(Block block, File outputFile) throws IOException {
        if (expandTransactions) {
            String blockJson = Block.JSON.toJSON(block);
            // get iterator over all transactions
            final Iterator<String> transactionBodyJsonIterator = block.items().stream()
                    .filter(BlockItem::hasEventTransaction)
                    .filter(item -> item.eventTransaction().hasApplicationTransaction())
                    .map(item -> {
                        try {
                            return "          " + TransactionBody.JSON.toJSON(
                                            TransactionBody.PROTOBUF.parse(
                                                    SignedTransaction.PROTOBUF.parse(
                                                                    Transaction.PROTOBUF.parse(
                                                                                    item.eventTransaction().applicationTransaction())
                                                                            .signedTransactionBytes())
                                                            .bodyBytes()))
                                    .replaceAll("\n", "\n          ");
                        } catch (ParseException e) {
                            System.err.println("Error parsing transaction body : "+e.getMessage());
                            throw new RuntimeException(e);
                        }
                    })
                    .iterator();
            // find all "applicationTransaction" fields and expand them, replacing with json from iterator
            blockJson = Pattern.compile("(\"applicationTransaction\": )\"([^\"]+)\"")
                    .matcher(blockJson)
                    .replaceAll(matchResult -> matchResult.group(1) + transactionBodyJsonIterator.next());
            Files.writeString(outputFile.toPath(), blockJson);
        } else {
            try (OutputStream fOut = new BufferedOutputStream(new FileOutputStream(outputFile), 1024 * 1024)) {
                Block.JSON.write(block, new WritableStreamingData(fOut));
            }
        }
    }
}
