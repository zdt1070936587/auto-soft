package com.autosoft.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenCode配置属性。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@ConfigurationProperties(prefix = "autosoft.opencode")
public class OpenCodeProperties {

    private String baseUrl = "https://opencode.ai/zen/go/v1";
    private int timeoutSeconds = 120;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
