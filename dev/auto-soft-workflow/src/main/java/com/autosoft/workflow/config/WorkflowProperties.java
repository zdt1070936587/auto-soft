package com.autosoft.workflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow配置属性。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@ConfigurationProperties(prefix = "autosoft.workflow")
public class WorkflowProperties {

    private final Http http = new Http();
    private final Schedule schedule = new Schedule();

    public Http getHttp() {
        return http;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public static class Http {
        private List<String> allowedHosts = new ArrayList<>();

        public List<String> getAllowedHosts() {
            return allowedHosts;
        }

        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = allowedHosts == null ? new ArrayList<>() : allowedHosts;
        }
    }

    public static class Schedule {
        private long minIntervalMs = 300_000L;

        public long getMinIntervalMs() {
            return minIntervalMs;
        }

        public void setMinIntervalMs(long minIntervalMs) {
            this.minIntervalMs = minIntervalMs;
        }
    }
}
