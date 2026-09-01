package com.autosoft.system.log;

import com.autosoft.system.config.PageVisitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PageVisitRetention定时任务。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class PageVisitRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(PageVisitRetentionJob.class);

    private final PageVisitProperties properties;
    private final PageVisitService pageVisitService;

    public PageVisitRetentionJob(PageVisitProperties properties, PageVisitService pageVisitService) {
        this.properties = properties;
        this.pageVisitService = pageVisitService;
    }

    @Scheduled(cron = "${autosoft.telemetry.page-visit.retention-cron:0 30 3 * * ?}")
    public void run() {
        if (!properties.isEnabled() || properties.getRetentionDays() <= 0) {
            return;
        }
        try {
            int removed = pageVisitService.purgeOlderThanRetention();
            if (removed > 0) {
                log.info("page visit retention removed {} rows", removed);
            }
        } catch (RuntimeException ex) {
            log.warn("page visit retention failed", ex);
        }
    }
}
