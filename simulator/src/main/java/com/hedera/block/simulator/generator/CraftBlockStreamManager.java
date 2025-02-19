// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.simulator.generator;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.requireNonNull;

import com.hedera.block.common.hasher.Hashes;
import com.hedera.block.common.hasher.HashingUtilities;
import com.hedera.block.common.hasher.NaiveStreamingTreeHasher;
import com.hedera.block.common.hasher.StreamingTreeHasher;
import com.hedera.block.simulator.config.data.BlockGeneratorConfig;
import com.hedera.block.simulator.config.types.GenerationMode;
import com.hedera.block.simulator.exception.BlockSimulatorParsingException;
import com.hedera.block.simulator.generator.itemhandler.BlockHeaderHandler;
import com.hedera.block.simulator.generator.itemhandler.BlockProofHandler;
import com.hedera.block.simulator.generator.itemhandler.EventHeaderHandler;
import com.hedera.block.simulator.generator.itemhandler.EventTransactionHandler;
import com.hedera.block.simulator.generator.itemhandler.ItemHandler;
import com.hedera.block.simulator.generator.itemhandler.TransactionResultHandler;
import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.protoc.Block;
import com.hedera.hapi.block.stream.protoc.BlockItem;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CraftBlockStreamManager implements BlockStreamManager {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    // Service
    private final Random random;

    // Configuration
    private final int minNumberOfEventsPerBlock;
    private final int maxNumberOfEventsPerBlock;
    private final int minNumberOfTransactionsPerEvent;
    private final int maxNumberOfTransactionsPerEvent;

    // State
    private final GenerationMode generationMode;
    private final byte[] previousStateRootHash;
    private byte[] previousBlockHash;
    private byte[] currentBlockHash;
    private long currentBlockNumber;
    private StreamingTreeHasher inputTreeHasher;
    private StreamingTreeHasher outputTreeHasher;

    public CraftBlockStreamManager(@NonNull BlockGeneratorConfig blockGeneratorConfig) {
        final BlockGeneratorConfig blockGeneratorConfig1 = requireNonNull(blockGeneratorConfig);
        this.generationMode = blockGeneratorConfig1.generationMode();
        this.currentBlockNumber = blockGeneratorConfig1.startBlockNumber();
        this.minNumberOfEventsPerBlock = blockGeneratorConfig1.minNumberOfEventsPerBlock();
        this.maxNumberOfEventsPerBlock = blockGeneratorConfig1.maxNumberOfEventsPerBlock();
        this.minNumberOfTransactionsPerEvent = blockGeneratorConfig1.minNumberOfTransactionsPerEvent();
        this.maxNumberOfTransactionsPerEvent = blockGeneratorConfig1.maxNumberOfTransactionsPerEvent();

        this.random = new Random();
        this.previousStateRootHash = new byte[StreamingTreeHasher.HASH_LENGTH];

        this.previousBlockHash = new byte[StreamingTreeHasher.HASH_LENGTH];
        this.currentBlockHash = new byte[StreamingTreeHasher.HASH_LENGTH];

        this.inputTreeHasher = new NaiveStreamingTreeHasher();
        this.outputTreeHasher = new NaiveStreamingTreeHasher();

        LOGGER.log(INFO, "Block Stream Simulator will use Craft mode for block management.");
    }

    @Override
    public GenerationMode getGenerationMode() {
        return generationMode;
    }

    @Override
    public BlockItem getNextBlockItem() {
        throw new UnsupportedOperationException("Craft mode does not support getting block items");
    }

    @Override
    public Block getNextBlock() throws IOException, BlockSimulatorParsingException {
        LOGGER.log(DEBUG, "Started creation of block number %s.".formatted(currentBlockNumber));
        final List<BlockItemUnparsed> blockItemsUnparsed = new ArrayList<>();
        final List<ItemHandler> items = new ArrayList<>();

        final ItemHandler headerItemHandler = new BlockHeaderHandler(previousBlockHash, currentBlockNumber);
        items.add(headerItemHandler);
        blockItemsUnparsed.add(headerItemHandler.unparseBlockItem());

        final int eventsNumber = random.nextInt(minNumberOfEventsPerBlock, maxNumberOfEventsPerBlock);
        for (int i = 0; i < eventsNumber; i++) {
            final ItemHandler eventHeaderHandler = new EventHeaderHandler();
            items.add(eventHeaderHandler);
            blockItemsUnparsed.add(eventHeaderHandler.unparseBlockItem());

            final int transactionsNumber =
                    random.nextInt(minNumberOfTransactionsPerEvent, maxNumberOfTransactionsPerEvent);
            for (int j = 0; j < transactionsNumber; j++) {
                final ItemHandler eventTransactionHandler = new EventTransactionHandler();
                items.add(eventTransactionHandler);
                blockItemsUnparsed.add(eventTransactionHandler.unparseBlockItem());

                final ItemHandler transactionResultHandler = new TransactionResultHandler();
                items.add(transactionResultHandler);
                blockItemsUnparsed.add(transactionResultHandler.unparseBlockItem());
            }
        }

        LOGGER.log(DEBUG, "Appending %s number of block items in this block.".formatted(items.size()));

        processBlockItems(blockItemsUnparsed);
        updateCurrentBlockHash();

        ItemHandler proofItemHandler = new BlockProofHandler(previousBlockHash, currentBlockHash, currentBlockNumber);
        items.add(proofItemHandler);
        resetState();
        return Block.newBuilder()
                .addAllItems(items.stream().map(ItemHandler::getItem).toList())
                .build();
    }

    private void updateCurrentBlockHash() {
        com.hedera.hapi.block.stream.BlockProof unfinishedBlockProof =
                com.hedera.hapi.block.stream.BlockProof.newBuilder()
                        .previousBlockRootHash(Bytes.wrap(previousBlockHash))
                        .startOfBlockStateRootHash(Bytes.wrap(previousStateRootHash))
                        .build();

        currentBlockHash = HashingUtilities.computeFinalBlockHash(
                        unfinishedBlockProof, inputTreeHasher, outputTreeHasher)
                .toByteArray();
    }

    private void processBlockItems(List<BlockItemUnparsed> blockItems) {
        Hashes hashes = HashingUtilities.getBlockHashes(blockItems);
        while (hashes.inputHashes().hasRemaining()) {
            inputTreeHasher.addLeaf(hashes.inputHashes());
        }
        while (hashes.outputHashes().hasRemaining()) {
            outputTreeHasher.addLeaf(hashes.outputHashes());
        }
    }

    private void resetState() {
        inputTreeHasher = new NaiveStreamingTreeHasher();
        outputTreeHasher = new NaiveStreamingTreeHasher();
        currentBlockNumber++;
        previousBlockHash = currentBlockHash;
    }
}
