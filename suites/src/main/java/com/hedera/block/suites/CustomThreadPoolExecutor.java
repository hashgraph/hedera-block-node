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

package com.hedera.block.suites;

import static java.lang.System.Logger.Level.ERROR;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A custom implementation of {@link ThreadPoolExecutor} that provides specific configurations
 * and enhanced error logging capabilities for tasks executed within the thread pool.
 *
 * <p>This executor is configured with:
 * <ul>
 *   <li>Core pool size: 8 threads
 *   <li>Maximum pool size: 8 threads
 *   <li>Keep-alive time: 10 seconds
 *   <li>Work queue: {@link LinkedBlockingQueue} with default capacity
 * </ul>
 *
 * <p>The executor overrides the {@link #afterExecute(Runnable, Throwable)} method to log any
 * exceptions thrown by tasks after their execution, aiding in debugging and error tracking.
 */
public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
    private final System.Logger LOGGER = System.getLogger(getClass().getName());

    /**
     * Constructs a new {@code CustomThreadPoolExecutor} with a fixed thread pool configuration.
     *
     * <p>The executor is set up with:
     * <ul>
     *   <li>Core pool size of 8 threads: the minimum number of threads to keep in the pool.
     *   <li>Maximum pool size of 8 threads: the maximum number of threads allowed in the pool.
     *   <li>Keep-alive time of 10 seconds: the maximum time that excess idle threads will wait for new tasks before terminating.
     *   <li>Work queue: a {@link LinkedBlockingQueue} to hold tasks before they are executed.
     * </ul>
     *
     * <p>This configuration ensures that the thread pool maintains 8 threads and does not create additional threads beyond that number.
     */
    public CustomThreadPoolExecutor() {
        super(8, 8, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    /**
     * Invoked after the execution of the given {@code Runnable} task. This method provides an opportunity to perform
     * actions after each task execution, such as logging exceptions thrown by the task.
     *
     * <p>If the task execution resulted in an exception, this method logs the error message using the {@code System.Logger}.
     *
     * @param r the runnable task that has completed execution
     * @param t the exception that caused termination, or {@code null} if execution completed normally
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t != null) {
            LOGGER.log(ERROR, "Task encountered an error: " + t.getMessage());
        }
    }
}
