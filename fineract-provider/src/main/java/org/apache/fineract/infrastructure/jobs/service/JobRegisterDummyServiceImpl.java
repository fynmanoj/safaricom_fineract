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
package org.apache.fineract.infrastructure.jobs.service;

import javax.annotation.PostConstruct;

import org.apache.fineract.infrastructure.core.exception.PlatformInternalServerException;
import org.apache.fineract.infrastructure.jobs.domain.ScheduledJobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

/**
 * Dummy Service class for Scheduler 
 * {@link SchedulerFactoryBean} ,{@link MethodInvokingJobDetailFactoryBean} and
 * {@link CronTriggerFactoryBean}
 */
@Service
@ConditionalOnProperty("job.register.dummy.service.enabled")
public class JobRegisterDummyServiceImpl implements JobRegisterService, ApplicationListener<ContextClosedEvent> {

    private final static Logger logger = LoggerFactory.getLogger(JobRegisterDummyServiceImpl.class);



    @PostConstruct
    public void loadAllJobs() {
        
    }

    public void executeJob(final ScheduledJobDetail scheduledJobDetail, String triggerType) {
        
    	throw new PlatformInternalServerException("error.msg.sheduler.job.execution.failed", "Current node not running scheduler", scheduledJobDetail.getId());
    }

    public void rescheduleJob(final ScheduledJobDetail scheduledJobDetail) {
    	throw new PlatformInternalServerException("error.msg.sheduler.job.reschedule.failed", "Current node not running scheduler", scheduledJobDetail.getId());
    }

    @Override
    public void pauseScheduler() {
    	throw new PlatformInternalServerException("error.msg.sheduler.pause.failed", "Current node not running scheduler");
    }

    @Override
    public void startScheduler() {
    	throw new PlatformInternalServerException("error.msg.sheduler.start.failed", "Current node not running scheduler");
    }

    @Override
    public void rescheduleJob(final Long jobId) {
    	throw new PlatformInternalServerException("error.msg.sheduler.job.reschedule.failed", "Current node not running scheduler", jobId);
    }

    @Override
    public void executeJob(final Long jobId) {
    	throw new PlatformInternalServerException("error.msg.sheduler.job.execution.failed", "Current node not running scheduler", jobId);
    }

    @Override
    public boolean isSchedulerRunning() {
    	throw new PlatformInternalServerException("error.msg.sheduler.isrunning.failed", "Current node not running scheduler");
    }

    @Override
    public void onApplicationEvent(@SuppressWarnings("unused") ContextClosedEvent event) {
    	throw new PlatformInternalServerException("error.msg.sheduler.on.app.event.failed", "Current node not running scheduler");
    }

    @Override
    public void stopAllSchedulers() {
    	throw new PlatformInternalServerException("error.msg.sheduler.stop.all.failed", "Current node not running scheduler");
    }

    @Override
    public void stopScheduler(final String name) {
    	throw new PlatformInternalServerException("error.msg.sheduler.stop.failed", "Current node not running scheduler", name);
    }
}
