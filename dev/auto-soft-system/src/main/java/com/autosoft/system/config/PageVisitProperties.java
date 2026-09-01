package com.autosoft.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PageVisit配置属性。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@ConfigurationProperties(prefix = "autosoft.telemetry.page-visit")
public class PageVisitProperties {

    private boolean enabled = true;
    private int retentionDays = 90;
    private int maxBatchSize = 50;
    private int dedupeWindowSeconds = 60;
    private String retentionCron = "0 30 3 * * ?";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public int getDedupeWindowSeconds() {
        return dedupeWindowSeconds;
    }

    public void setDedupeWindowSeconds(int dedupeWindowSeconds) {
        this.dedupeWindowSeconds = dedupeWindowSeconds;
    }

    public String getRetentionCron() {
        return retentionCron;
    }

    public void setRetentionCron(String retentionCron) {
        this.retentionCron = retentionCron;
    }
}
