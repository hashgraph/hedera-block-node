// SPDX-License-Identifier: Apache-2.0
package org.hiero.block.server.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.pbj.runtime.OneOf;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.hiero.block.internal.BlockItemUnparsed;
import org.hiero.block.internal.BlockItemUnparsed.ItemOneOfType;
import org.hiero.block.node.app.fixtures.async.BlockingExecutor;
import org.hiero.block.node.app.fixtures.async.ScheduledBlockingExecutor;
import org.hiero.block.node.app.fixtures.async.TestThreadPoolManager;
import org.hiero.block.node.messaging.BlockMessagingFacilityImpl;
import org.hiero.block.node.messaging.MessagingConfig;
import org.hiero.block.node.spi.BlockNodeContext;
import org.hiero.block.node.spi.blockmessaging.BlockItemHandler;
import org.hiero.block.node.spi.blockmessaging.BlockItems;
import org.hiero.block.node.spi.blockmessaging.BlockMessagingFacility;
import org.hiero.block.node.spi.blockmessaging.BlockNotificationHandler;
import org.hiero.block.node.spi.blockmessaging.BlockSource;
import org.hiero.block.node.spi.blockmessaging.PersistedNotification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that an exception thrown by a message handler is logged and does not stop that handler or stall the ring
 * buffer it gates.
 */
public class FacilityExceptionTest {

    /** How long to wait for handlers to receive all messages. */
    private static final long AWAIT_TIMEOUT_SECONDS = 10;

    /** The logger used for logging messages. */
    private java.util.logging.Logger logger;
    /** The custom log handler used to capture log messages. */
    private TestLogHandler logHandler;
    /** The thread pool manager to use when testing */
    private TestThreadPoolManager<BlockingExecutor, ScheduledBlockingExecutor> threadPoolManager;
    /** The messaging facility under test */
    private BlockMessagingFacility service;
    /** The ring buffer sizes used by the facility under test */
    private MessagingConfig messagingConfig;

    /**
     * Set up the test environment by initializing the logger and adding a custom log handler so that we can capture
     * log messages.
     */
    @BeforeEach
    void setUp() {
        logger = java.util.logging.Logger.getLogger(BlockMessagingFacilityImpl.class.getName());
        logHandler = new TestLogHandler();
        logger.addHandler(logHandler);
        logger.setLevel(Level.INFO);
        threadPoolManager = new TestThreadPoolManager<>(
                new BlockingExecutor(new LinkedBlockingQueue<>()),
                new ScheduledBlockingExecutor(new LinkedBlockingQueue<>()));
        final BlockNodeContext context = TestConfig.generateContext(threadPoolManager);
        messagingConfig = context.configuration().getConfigData(MessagingConfig.class);
        service = new BlockMessagingFacilityImpl();
        service.init(context, null);
    }

    @AfterEach
    void tearDown() {
        service.stop();
        logger.removeHandler(logHandler);
    }

    /**
     * Test to verify that a log message is captured in testing
     */
    @Test
    void testLogMessage() {
        String expectedMessage = "Test log message";
        logger.info(expectedMessage);
        assertTrue(logHandler.containsMessage(expectedMessage), "Log message should be captured");
        // tests with system logger as well
        final System.Logger systemLogger = System.getLogger(BlockMessagingFacilityImpl.class.getName());
        String expectedMessage2 = "SYSTEM-MESSAGE";
        systemLogger.log(System.Logger.Level.INFO, expectedMessage2);
        assertTrue(logHandler.containsMessage(expectedMessage2), "Log message should be captured");
    }

    /**
     * A gating block item handler that always throws must not stall the ring: a second gating handler keeps receiving
     * every batch, even after the ring has wrapped around more than once.
     */
    @Test
    void throwingBlockItemHandlerDoesNotStallRing() throws InterruptedException {
        final int totalBatches = messagingConfig.blockItemQueueSize() * 2 + 1;
        final CountDownLatch allReceived = new CountDownLatch(totalBatches);
        service.registerBlockItemHandler(new ThrowingBlockItemHandler(), false, "throwing");
        service.registerBlockItemHandler(new CountingBlockItemHandler(allReceived), false, "counting");
        service.start();

        sendBlockItemsInBackground(totalBatches);

        assertTrue(
                allReceived.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Counting handler stopped receiving batches; missing " + allReceived.getCount());
    }

    /**
     * A gating block notification handler that always throws must not stall the ring: a second gating handler keeps
     * receiving every notification, even after the ring has wrapped around more than once.
     */
    @Test
    void throwingBlockNotificationHandlerDoesNotStallRing() throws InterruptedException {
        final int totalNotifications = messagingConfig.blockNotificationQueueSize() * 2 + 1;
        final CountDownLatch allReceived = new CountDownLatch(totalNotifications);
        service.registerBlockNotificationHandler(new ThrowingBlockNotificationHandler(), false, "throwing");
        service.registerBlockNotificationHandler(new CountingBlockNotificationHandler(allReceived), false, "counting");
        service.start();

        for (int blockNumber = 0; blockNumber < totalNotifications; blockNumber++) {
            service.sendBlockPersisted(new PersistedNotification(blockNumber, true, 1, BlockSource.PUBLISHER));
        }
        threadPoolManager
                .executor()
                .executeAsync(false, 10_000L, false, false, () -> Executors.newSingleThreadExecutor());

        assertTrue(
                allReceived.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Counting handler stopped receiving notifications; missing " + allReceived.getCount());
    }

    /**
     * A handler that throws on one batch skips only that batch and still receives the batches after it.
     */
    @Test
    void handlerReceivesBatchesAfterOneThatThrew() throws InterruptedException {
        final CountDownLatch laterBatchesReceived = new CountDownLatch(2);
        service.registerBlockItemHandler(new ThrowOnFirstBlockItemHandler(laterBatchesReceived), false, "first-fails");
        service.start();

        sendBlockItemsInBackground(3);

        assertTrue(
                laterBatchesReceived.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Handler stopped receiving batches after the first one threw");
    }

    /**
     * A handler failure is logged at ERROR with the handler name and the original exception, not only as an uncaught
     * exception of a dying thread.
     */
    @Test
    void handlerFailureIsLoggedAtErrorWithHandlerNameAndOriginalException() throws InterruptedException {
        final CountDownLatch laterBatchesReceived = new CountDownLatch(2);
        final ThrowOnFirstBlockItemHandler handler = new ThrowOnFirstBlockItemHandler(laterBatchesReceived);
        service.registerBlockItemHandler(handler, false, "first-fails");
        service.start();

        sendBlockItemsInBackground(3);
        assertTrue(
                laterBatchesReceived.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Handler stopped receiving batches after the first one threw");

        assertTrue(
                logHandler.containsRecord(Level.SEVERE, "first-fails", handler.thrownException()),
                "No ERROR log with the handler name and original exception: " + logHandler.messages());
    }

    /**
     * A handler interrupted by its own unregistration fails as part of a normal stop, so the failure is logged at
     * DEBUG and not at ERROR.
     */
    @Test
    void handlerInterruptedByUnregisterIsNotLoggedAtError() throws InterruptedException {
        logger.setLevel(Level.FINE);
        final BlockingBlockItemHandler handler = new BlockingBlockItemHandler();
        service.registerBlockItemHandler(handler, false, "interrupted-on-unregister");
        service.start();
        sendBlockItemsInBackground(1);
        assertTrue(handler.entered.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Handler never received the batch");

        service.unregisterBlockItemHandler(handler);
        assertTrue(
                logHandler.awaitRecord(Level.FINE, "interrupted-on-unregister", AWAIT_TIMEOUT_SECONDS),
                "No DEBUG log for the interrupted handler: " + logHandler.messages());

        assertFalse(
                logHandler.containsRecord(Level.SEVERE, "interrupted-on-unregister"),
                "Interrupted handler was logged at ERROR: " + logHandler.messages());
    }

    /**
     * Sends batches from a background thread, so a stalled ring blocks that thread instead of the test.
     *
     * @param totalBatches the number of batches to send
     */
    private void sendBlockItemsInBackground(final int totalBatches) {
        Thread.ofVirtual().start(new BlockItemsSender(service, totalBatches));
    }

    /**
     * Sends a fixed number of single-item batches to the facility.
     *
     * @param service the facility to send to
     * @param totalBatches the number of batches to send
     */
    private record BlockItemsSender(BlockMessagingFacility service, int totalBatches) implements Runnable {
        @Override
        public void run() {
            for (int blockNumber = 0; blockNumber < totalBatches; blockNumber++) {
                service.sendBlockItems(new BlockItems(
                        List.of(new BlockItemUnparsed(new OneOf<>(ItemOneOfType.BLOCK_HEADER, null))),
                        blockNumber,
                        true,
                        true));
            }
        }
    }

    /** Block item handler that always throws. */
    private static final class ThrowingBlockItemHandler implements BlockItemHandler {
        @Override
        public void handleBlockItemsReceived(final BlockItems blockItems) {
            throw new IllegalStateException("Simulated block item handler failure");
        }
    }

    /** Block item handler that counts down a latch for every batch received. */
    private record CountingBlockItemHandler(CountDownLatch received) implements BlockItemHandler {
        @Override
        public void handleBlockItemsReceived(final BlockItems blockItems) {
            received.countDown();
        }
    }

    /** Block item handler that throws on the first batch and counts down a latch for every later batch. */
    private static final class ThrowOnFirstBlockItemHandler implements BlockItemHandler {
        private final CountDownLatch laterBatchesReceived;
        private final AtomicBoolean thrown = new AtomicBoolean(false);
        private final IllegalStateException thrownException =
                new IllegalStateException("Simulated failure on first batch");

        private ThrowOnFirstBlockItemHandler(final CountDownLatch laterBatchesReceived) {
            this.laterBatchesReceived = laterBatchesReceived;
        }

        @Override
        public void handleBlockItemsReceived(final BlockItems blockItems) {
            if (thrown.compareAndSet(false, true)) {
                throw thrownException;
            }
            laterBatchesReceived.countDown();
        }

        private IllegalStateException thrownException() {
            return thrownException;
        }
    }

    /**
     * Block item handler that blocks until interrupted and then rethrows the interrupt wrapped, which clears the
     * thread's interrupt flag as many real handlers do.
     */
    private static final class BlockingBlockItemHandler implements BlockItemHandler {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch neverReleased = new CountDownLatch(1);

        @Override
        public void handleBlockItemsReceived(final BlockItems blockItems) {
            entered.countDown();
            try {
                neverReleased.await();
            } catch (final InterruptedException e) {
                throw new IllegalStateException("Handler interrupted", e);
            }
        }
    }

    /** Block notification handler that always throws on persisted notifications. */
    private static final class ThrowingBlockNotificationHandler implements BlockNotificationHandler {
        @Override
        public void handlePersisted(final PersistedNotification notification) {
            throw new IllegalStateException("Simulated block notification handler failure");
        }
    }

    /** Block notification handler that counts down a latch for every persisted notification received. */
    private record CountingBlockNotificationHandler(CountDownLatch received) implements BlockNotificationHandler {
        @Override
        public void handlePersisted(final PersistedNotification notification) {
            received.countDown();
        }
    }

    /**
     * Custom log handler to capture log records for testing.
     */
    private static class TestLogHandler extends Handler {
        private final List<LogRecord> records = new CopyOnWriteArrayList<>();
        private final Semaphore published = new Semaphore(0);

        @Override
        public void publish(final LogRecord record) {
            records.add(record);
            published.release();
        }

        @Override
        public void flush() {
            // No-op
        }

        @Override
        public void close() throws SecurityException {
            // No-op
        }

        private boolean containsMessage(final String text) {
            for (final LogRecord record : records) {
                if (record.getMessage().contains(text)) {
                    return true;
                }
            }
            return false;
        }

        private boolean containsRecord(final Level level, final String text, final Throwable thrown) {
            for (final LogRecord record : records) {
                if (record.getLevel().equals(level)
                        && record.getMessage().contains(text)
                        && record.getThrown() == thrown) {
                    return true;
                }
            }
            return false;
        }

        private boolean containsRecord(final Level level, final String text) {
            for (final LogRecord record : records) {
                if (record.getLevel().equals(level) && record.getMessage().contains(text)) {
                    return true;
                }
            }
            return false;
        }

        private boolean awaitRecord(final Level level, final String text, final long timeoutSeconds)
                throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            while (!containsRecord(level, text)) {
                final long remaining = deadline - System.nanoTime();
                if (remaining <= 0 || !published.tryAcquire(remaining, TimeUnit.NANOSECONDS)) {
                    return false;
                }
            }
            return true;
        }

        private String messages() {
            final StringBuilder messages = new StringBuilder();
            for (final LogRecord record : records) {
                messages.append(record.getMessage()).append('\n');
            }
            return messages.toString();
        }
    }
}
