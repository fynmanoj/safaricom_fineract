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

import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class SavingsSchedularServiceImpl implements SavingsSchedularService {

    private static final Logger LOG = LoggerFactory.getLogger(SavingsSchedularServiceImpl.class);

    private final SavingsAccountAssembler savingAccountAssembler;
    private final SavingsAccountWritePlatformService savingsAccountWritePlatformService;
    private final SavingsAccountReadPlatformService savingAccountReadPlatformService;
    private final SavingsAccountRepositoryWrapper savingsAccountRepository;
    private final ConfigurationDomainService configurationDomainService;

    @Autowired
    public SavingsSchedularServiceImpl(final SavingsAccountAssembler savingAccountAssembler,
            final SavingsAccountWritePlatformService savingsAccountWritePlatformService,
            final SavingsAccountReadPlatformService savingAccountReadPlatformService,
            final ConfigurationDomainService configurationDomainService, final SavingsAccountRepositoryWrapper savingsAccountRepository) {
        this.savingAccountAssembler = savingAccountAssembler;
        this.savingsAccountWritePlatformService = savingsAccountWritePlatformService;
        this.savingAccountReadPlatformService = savingAccountReadPlatformService;
        this.savingsAccountRepository = savingsAccountRepository;
        this.configurationDomainService = configurationDomainService;
    }

    @Override
    @CronTarget(jobName = JobName.POST_INTEREST_FOR_SAVINGS)
    public void postInterestForAccounts() throws JobExecutionException {
        int page = 0;
        Integer initialSize = 500;
        Integer totalPageSize = 0;
        final StringBuffer sb = new StringBuffer();

        Integer nThreads = this.configurationDomainService.retrieveScheduledJobNumberOfThreads();

        CallerBlocksPolicy policy = new CallerBlocksPolicy(23); // 23 hours max wait time
        ExecutorService executor = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(nThreads), policy);

        final FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant();
        do {
            PageRequest pageRequest = new PageRequest(page, initialSize);
            Page<SavingsAccount> savingsAccounts = this.savingsAccountRepository.findByStatus(SavingsAccountStatusType.ACTIVE.getValue(),
                    pageRequest);

            for (SavingsAccount savingsAccount : savingsAccounts.getContent()) {
                try {
                    this.savingAccountAssembler.assignSavingAccountHelpers(savingsAccount);
                    boolean postInterestAsOn = false;
                    LocalDate transactionDate = null;
                    this.savingsAccountWritePlatformService.postInterest(savingsAccount, postInterestAsOn, transactionDate);
                } catch (Exception e) {
                    LOG.error("Failed to post interest for Savings with id {}", savingsAccount.getId(), e);
                    errors.add(e);
                }
            }
            page++;
            totalPageSize = savingsAccounts.getTotalPages();

            Runnable piTask = () -> {
                sb.append(postInterestForAccountsPage(tenant, this.savingAccountAssembler, this.savingsAccountWritePlatformService,
                        savingsAccounts));
            };

            executor.execute(piTask);

            page++;

        } while (page < totalPageSize);

        executor.shutdown();

        if (sb.length() > 0) {
            throw new JobExecutionException(sb.toString());
        }
    }

    private StringBuffer postInterestForAccountsPage(FineractPlatformTenant tenant, SavingsAccountAssembler saAssembler,
            SavingsAccountWritePlatformService saWritePlatformService, Page<SavingsAccount> savingsAccounts) {
        StringBuffer sbPage = new StringBuffer();
        ThreadLocalContextUtil.setTenant(tenant);
        for (SavingsAccount savingsAccount : savingsAccounts.getContent()) {
            try {
                saAssembler.assignSavingAccountHelpers(savingsAccount);
                boolean postInterestAsOn = false;
                LocalDate transactionDate = null;

                if (this.logger.isDebugEnabled()) {
                    String logString = "###  " + LocalDateTime.now() + " | Page: " + savingsAccounts.getNumber() + " | Account: "
                            + savingsAccount.getAccountNumber();
                    this.logger.debug(logString);
                }

                saWritePlatformService.postInterest(savingsAccount, postInterestAsOn, transactionDate);
            } catch (Exception e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.error("PostInterest failed.", e);
                }

                Throwable realCause = e;
                if (e.getCause() != null) {
                    realCause = e.getCause();
                }
                sbPage.append("failed to post interest for Savings with id " + savingsAccount.getId() + " with message "
                        + realCause.getMessage());
            }
        }
        return sbPage;
    }

    @Override
    @CronTarget(jobName = JobName.UPDATE_SAVINGS_DORMANT_ACCOUNTS)
    public void updateSavingsDormancyStatus() throws JobExecutionException {
        LocalDate tenantLocalDate = DateUtils.getLocalDateOfTenant();

        List<Long> savingsPendingInactive = savingAccountReadPlatformService.retrieveSavingsIdsPendingInactive(tenantLocalDate);
        if (null != savingsPendingInactive && savingsPendingInactive.size() > 0) {
            for (Long savingsId : savingsPendingInactive) {
                this.savingsAccountWritePlatformService.setSubStatusInactive(savingsId);
            }
        }

        List<Long> savingsPendingDormant = savingAccountReadPlatformService.retrieveSavingsIdsPendingDormant(tenantLocalDate);
        if (null != savingsPendingDormant && savingsPendingDormant.size() > 0) {
            for (Long savingsId : savingsPendingDormant) {
                this.savingsAccountWritePlatformService.setSubStatusDormant(savingsId);
            }
        }

        List<Long> savingsPendingEscheat = savingAccountReadPlatformService.retrieveSavingsIdsPendingEscheat(tenantLocalDate);
        if (null != savingsPendingEscheat && savingsPendingEscheat.size() > 0) {
            for (Long savingsId : savingsPendingEscheat) {
                this.savingsAccountWritePlatformService.escheat(savingsId);
            }
        }
    }

}

class CallerBlocksPolicy implements RejectedExecutionHandler {

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
