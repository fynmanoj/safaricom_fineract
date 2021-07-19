/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.savings.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerBlocksPolicy implements RejectedExecutionHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(CallerBlocksPolicy.class);

    private final long maxWait;

    /**
     * Construct instance based on the provided maximum wait time.
     *
     * @param maxWait
     *            The maximum time to wait for a queue slot to be available, in milliseconds.
     */
    public CallerBlocksPolicy(long maxWait) {
        this.maxWait = maxWait;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (!executor.isShutdown()) {
            try {
                BlockingQueue<Runnable> queue = executor.getQueue();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Attempting to queue task execution for " + this.maxWait + " milliseconds");
                }
                if (!queue.offer(r, this.maxWait, TimeUnit.HOURS)) {
                    throw new RejectedExecutionException("Max wait time expired to queue task");
                }
                LOGGER.debug("Task execution queued");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted", e);
            }
        } else {
            throw new RejectedExecutionException("Executor has been shut down");
        }
    }

}
