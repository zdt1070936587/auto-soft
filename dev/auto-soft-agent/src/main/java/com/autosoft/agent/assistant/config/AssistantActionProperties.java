package com.autosoft.agent.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 助手操作 Copilot 配置属性。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@ConfigurationProperties(prefix = "autosoft.assistant.action")
public class AssistantActionProperties {

    private int actionDraftTtlMinutes = 30;
    private int capabilityAmbiguousScoreGap = 15;

    public int getActionDraftTtlMinutes() {
        return actionDraftTtlMinutes;
    }

    public void setActionDraftTtlMinutes(int actionDraftTtlMinutes) {
        this.actionDraftTtlMinutes = actionDraftTtlMinutes;
    }

    public int getCapabilityAmbiguousScoreGap() {
        return capabilityAmbiguousScoreGap;
    }

    public void setCapabilityAmbiguousScoreGap(int capabilityAmbiguousScoreGap) {
        this.capabilityAmbiguousScoreGap = capabilityAmbiguousScoreGap;
    }
}
